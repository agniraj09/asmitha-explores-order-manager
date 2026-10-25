package com.asmithaexplores.ordertracker.tracking;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Talks to the Franch Express tracking endpoint that backs their public
 * "courier tracking" page. It answers a JSON POST of {@code {"awb":"..."}} with the parcel's
 * current state and its scan history - the waybill number is the only input it looks at.
 * <p>
 * Everything here is best effort: an unreachable courier, a changed payload or an unknown waybill
 * all come back as {@link Optional#empty()} so a failing lookup can never change an order.
 */
@Component
public class FranchExpressTrackingClient implements CourierTrackingClient {

    private static final Logger log = LoggerFactory.getLogger(FranchExpressTrackingClient.class);

    /** "2026-10-24 22:36:42" - used by the scan history. */
    private static final DateTimeFormatter EVENT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
    /** "24-10-2026 10:36 PM" - used by the booking and delivery timestamps. */
    private static final DateTimeFormatter STAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy h:mm a", Locale.ENGLISH);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public FranchExpressTrackingClient(RestTemplateBuilder builder,
                                       ObjectMapper objectMapper,
                                       AppProperties appProperties) {
        // The sync runs on the single scheduler thread - a hung socket must not block later orders.
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    @Override
    public Optional<TrackingSnapshot> track(String trackingId) {
        if (!StringUtils.hasText(trackingId)) {
            return Optional.empty();
        }
        AppProperties.Tracking config = appProperties.getTracking();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            // The waybill number is the only thing the endpoint looks at.
            String body = objectMapper.writeValueAsString(Map.of("awb", trackingId.trim()));

            String response = restTemplate.postForObject(
                    config.getApiUrl(), new HttpEntity<>(body, headers), String.class);
            return parse(response);
        } catch (Exception e) {
            log.warn("Courier lookup for tracking id {} failed: {}", trackingId, e.toString());
            return Optional.empty();
        }
    }

    /** Package private so the payload mapping can be tested without calling the courier. */
    Optional<TrackingSnapshot> parse(String json) {
        if (!StringUtils.hasText(json)) {
            return Optional.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!"success".equalsIgnoreCase(root.path("status").asText())) {
                return Optional.empty();
            }
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                return Optional.empty();
            }

            String statusText = text(data.path("dl_status_txt"));
            LocalDateTime deliveredAt = parseDate(text(data.path("delv_dtm")), STAMP_FORMAT);
            List<TrackingEvent> events = new ArrayList<>();
            for (JsonNode node : data.path("tracking")) {
                String description = clean(text(node.path("trans_for")));
                if (!StringUtils.hasText(description)) {
                    continue;
                }
                events.add(new TrackingEvent(
                        parseDate(text(node.path("trans_dtm")), EVENT_FORMAT),
                        description,
                        clean(text(node.path("from")))));
            }

            boolean delivered = isDelivered(statusText, deliveredAt, events);
            if (delivered && deliveredAt == null) {
                TrackingEvent latest = events.isEmpty() ? null : events.get(0);
                deliveredAt = latest == null ? null : latest.at();
            }
            return Optional.of(new TrackingSnapshot(delivered, statusText, deliveredAt, events));
        } catch (Exception e) {
            log.warn("Could not read the courier tracking response: {}", e.toString());
            return Optional.empty();
        }
    }

    /**
     * The courier says "Delivered" for a handed over parcel and "Un Delivered" while it is still
     * moving, so the wording is compared exactly - a {@code contains} check would match both.
     * A delivery timestamp or a "Delivered" scan counts as well, in case the headline status
     * lags behind the scan history.
     */
    private static boolean isDelivered(String statusText, LocalDateTime deliveredAt, List<TrackingEvent> events) {
        if (deliveredAt != null || isDeliveredWording(statusText)) {
            return true;
        }
        return events.stream().anyMatch(e -> isDeliveredWording(e.description()));
    }

    private static boolean isDeliveredWording(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String normalised = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        return normalised.equals("delivered") || normalised.equals("deliveredtoconsignee");
    }

    private static LocalDateTime parseDate(String value, DateTimeFormatter formatter) {
        if (!StringUtils.hasText(value) || "-".equals(value.trim())) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim().replaceAll("\\s+", " "), formatter);
        } catch (Exception e) {
            log.debug("Unparsable courier date '{}'", value);
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    /** The courier embeds markup and padding in its labels - strip it before storing or showing. */
    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("(?i)<br\\s*/?>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
