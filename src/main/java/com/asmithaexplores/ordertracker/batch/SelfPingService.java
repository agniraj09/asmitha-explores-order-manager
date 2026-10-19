package com.asmithaexplores.ordertracker.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Keeps the free-tier Render instance awake.
 * <p>
 * Render spins a free web service down after ~15 minutes without traffic, and the
 * cold start that follows takes close to a minute. Calling our own {@code /ping}
 * endpoint on a shorter interval keeps the container warm.
 * <p>
 * Only active when {@code app.self-ping.enabled=true} (set on the prod profile), so
 * local runs and the test suite never fire requests at the deployed environment.
 */
@Component
@ConditionalOnProperty(prefix = "app.self-ping", name = "enabled", havingValue = "true")
public class SelfPingService {

    private static final Logger log = LoggerFactory.getLogger(SelfPingService.class);

    private final RestTemplate restTemplate;
    private final String pingUrl;

    public SelfPingService(RestTemplateBuilder builder,
                           @Value("${app.self-ping.url}") String pingUrl) {
        // Short timeouts: the scheduler runs on a single thread, so a hung socket
        // would otherwise block every later ping.
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
        this.pingUrl = pingUrl;
    }

    /**
     * Interval must stay below Render's ~15 minute idle timeout.
     * The initial delay gives the application time to finish starting up.
     */
    @Scheduled(initialDelayString = "${app.self-ping.initial-delay:60000}",
               fixedRateString = "${app.self-ping.interval:600000}")
    public void pingSelf() {
        try {
            String response = restTemplate.getForObject(pingUrl, String.class);
            log.debug("Self-ping OK: {}", response);
        } catch (Exception e) {
            // Best-effort housekeeping - never let it escape and pollute the logs as an error.
            log.warn("Self-ping to {} failed: {}", pingUrl, e.getMessage());
        }
    }
}
