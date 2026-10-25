package com.asmithaexplores.ordertracker.tracking;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.asmithaexplores.ordertracker.dto.OrderView;
import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Asks the courier what happened to the parcels of shipped orders and moves the ones that have
 * arrived to {@link OrderStatus#DELIVERED} without anybody having to press a button.
 * <p>
 * Runs on a timer and is also triggered from the order page ("Check courier status"). A courier
 * that cannot be reached, or that knows nothing about a waybill, leaves the order untouched.
 */
@Service
@RequiredArgsConstructor
public class DeliveryStatusSyncService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStatusSyncService.class);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CourierTrackingClient trackingClient;
    private final AppProperties appProperties;

    /**
     * Outcome of one lookup.
     *
     * @param courierAnswered the courier returned a usable status - false means "no news"
     * @param newlyDelivered  this lookup is what moved the order to delivered
     * @param order           the order as it stands now, {@code null} when the courier said nothing
     */
    public record SyncResult(boolean courierAnswered, boolean newlyDelivered, OrderView order) {

        static SyncResult noNews() {
            return new SyncResult(false, false, null);
        }
    }

    /** Looks one order up on demand, whatever the background sync is configured to do. */
    public SyncResult syncOrder(Long orderId) {
        Order order = orderService.getEntity(orderId);
        return sync(order.getId(), order.getTrackingId(), order.getOrderStatus());
    }

    @Scheduled(initialDelayString = "${app.tracking.initial-delay-ms:120000}",
               fixedDelayString = "${app.tracking.interval-ms:21600000}")
    public void syncShippedOrders() {
        AppProperties.Tracking config = appProperties.getTracking();
        if (!config.isAutoSyncEnabled()) {
            return;
        }
        LocalDate oldestToCheck = LocalDate.now().minusDays(Math.max(0, config.getMaxAgeDays()));
        List<Order> candidates = orderRepository
                .findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus.SHIPPED)
                .stream()
                .filter(o -> o.getOrderDate() == null || !o.getOrderDate().isBefore(oldestToCheck))
                .toList();
        if (candidates.isEmpty()) {
            return;
        }

        log.info("Delivery sync: checking {} shipped order(s) with the courier", candidates.size());
        int delivered = 0;
        for (Order order : candidates) {
            try {
                if (sync(order.getId(), order.getTrackingId(), order.getOrderStatus()).newlyDelivered()) {
                    delivered++;
                }
            } catch (Exception e) {
                // One bad order must never stop the rest of the run.
                log.warn("Delivery sync failed for order #{}: {}", order.getId(), e.toString());
            }
            pauseBetweenLookups(config.getRequestDelayMs());
        }
        log.info("Delivery sync finished: {} order(s) marked as delivered", delivered);
    }

    private SyncResult sync(Long orderId, String trackingId, OrderStatus statusBefore) {
        Optional<TrackingSnapshot> snapshot = trackingClient.track(trackingId);
        if (snapshot.isEmpty()) {
            return SyncResult.noNews();
        }
        OrderView updated = orderService.applyTrackingSnapshot(orderId, snapshot.get());
        boolean newlyDelivered = updated.getOrderStatus() == OrderStatus.DELIVERED
                && statusBefore != OrderStatus.DELIVERED;
        if (newlyDelivered) {
            log.info("Order #{} (tracking id {}) was delivered - status updated automatically",
                    orderId, trackingId);
        }
        return new SyncResult(true, newlyDelivered, updated);
    }

    private static void pauseBetweenLookups(long delayMs) {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
