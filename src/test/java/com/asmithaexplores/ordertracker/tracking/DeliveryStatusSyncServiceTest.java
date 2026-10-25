package com.asmithaexplores.ordertracker.tracking;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.asmithaexplores.ordertracker.dto.OrderView;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryStatusSyncServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderService orderService;
    @Mock
    private CourierTrackingClient trackingClient;

    private AppProperties appProperties;
    private DeliveryStatusSyncService syncService;

    @BeforeEach
    void setUp() {
        appProperties = new AppProperties();
        // No waiting around between lookups in the tests
        appProperties.getTracking().setRequestDelayMs(0);
        syncService = new DeliveryStatusSyncService(
                orderRepository, orderService, trackingClient, appProperties);
    }

    @Test
    @DisplayName("A delivered parcel moves its shipped order on")
    void deliveredParcelUpdatesTheOrder() {
        when(orderRepository.findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus.SHIPPED))
                .thenReturn(List.of(shippedOrder(100L, "22620340116", LocalDate.now().minusDays(2))));
        when(trackingClient.track("22620340116")).thenReturn(Optional.of(new TrackingSnapshot(
                true, "Delivered", LocalDateTime.now(), List.of())));
        when(orderService.applyTrackingSnapshot(eq(100L), any()))
                .thenReturn(OrderView.builder().id(100L).orderStatus(OrderStatus.DELIVERED).build());

        syncService.syncShippedOrders();

        verify(orderService).applyTrackingSnapshot(eq(100L), any(TrackingSnapshot.class));
    }

    @Test
    @DisplayName("A courier that says nothing leaves the order alone")
    void silentCourierLeavesTheOrderAlone() {
        when(orderRepository.findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus.SHIPPED))
                .thenReturn(List.of(shippedOrder(100L, "22620340116", LocalDate.now())));
        when(trackingClient.track("22620340116")).thenReturn(Optional.empty());

        syncService.syncShippedOrders();

        verify(orderService, never()).applyTrackingSnapshot(anyLong(), any());
    }

    @Test
    @DisplayName("Orders shipped long ago are not polled any more")
    void skipsOrdersBeyondTheMaximumAge() {
        appProperties.getTracking().setMaxAgeDays(45);
        when(orderRepository.findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus.SHIPPED))
                .thenReturn(List.of(shippedOrder(100L, "22620340116", LocalDate.now().minusDays(90))));

        syncService.syncShippedOrders();

        verifyNoInteractions(trackingClient);
        verify(orderService, never()).applyTrackingSnapshot(anyLong(), any());
    }

    @Test
    @DisplayName("Switching the automatic sync off stops the background run")
    void respectsTheAutoSyncSwitch() {
        appProperties.getTracking().setAutoSyncEnabled(false);

        syncService.syncShippedOrders();

        verifyNoInteractions(orderRepository, trackingClient, orderService);
    }

    @Test
    @DisplayName("One failing order does not stop the rest of the run")
    void keepsGoingAfterAFailure() {
        when(orderRepository.findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus.SHIPPED))
                .thenReturn(List.of(
                        shippedOrder(100L, "AWB-1", LocalDate.now()),
                        shippedOrder(101L, "AWB-2", LocalDate.now())));
        when(trackingClient.track("AWB-1")).thenThrow(new RuntimeException("courier down"));
        when(trackingClient.track("AWB-2")).thenReturn(Optional.of(new TrackingSnapshot(
                false, "Un Delivered", null, List.of())));
        when(orderService.applyTrackingSnapshot(eq(101L), any()))
                .thenReturn(OrderView.builder().id(101L).orderStatus(OrderStatus.SHIPPED).build());

        syncService.syncShippedOrders();

        verify(orderService).applyTrackingSnapshot(eq(101L), any(TrackingSnapshot.class));
    }

    @Test
    @DisplayName("The on-demand check reports when the courier had no answer")
    void manualCheckReportsNoNews() {
        Order order = shippedOrder(100L, "22620340116", LocalDate.now());
        when(orderService.getEntity(100L)).thenReturn(order);
        when(trackingClient.track("22620340116")).thenReturn(Optional.empty());

        DeliveryStatusSyncService.SyncResult result = syncService.syncOrder(100L);

        assertThat(result.courierAnswered()).isFalse();
        assertThat(result.newlyDelivered()).isFalse();
        assertThat(result.order()).isNull();
    }

    @Test
    @DisplayName("The on-demand check reports a fresh delivery")
    void manualCheckReportsDelivery() {
        Order order = shippedOrder(100L, "22620340116", LocalDate.now());
        when(orderService.getEntity(100L)).thenReturn(order);
        when(trackingClient.track("22620340116")).thenReturn(Optional.of(new TrackingSnapshot(
                true, "Delivered", LocalDateTime.now(), List.of())));
        when(orderService.applyTrackingSnapshot(eq(100L), any()))
                .thenReturn(OrderView.builder().id(100L).orderStatus(OrderStatus.DELIVERED).build());

        DeliveryStatusSyncService.SyncResult result = syncService.syncOrder(100L);

        assertThat(result.courierAnswered()).isTrue();
        assertThat(result.newlyDelivered()).isTrue();
    }

    private static Order shippedOrder(Long id, String trackingId, LocalDate orderDate) {
        return Order.builder()
                .id(id)
                .customer(Customer.builder().id(1L).name("Meera").build())
                .orderDate(orderDate)
                .orderStatus(OrderStatus.SHIPPED)
                .trackingId(trackingId)
                .build();
    }
}
