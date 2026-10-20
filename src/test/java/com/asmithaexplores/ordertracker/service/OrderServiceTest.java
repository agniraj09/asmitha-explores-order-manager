package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.OrderCreationResult;
import com.asmithaexplores.ordertracker.dto.OrderForm;
import com.asmithaexplores.ordertracker.dto.OrderItemForm;
import com.asmithaexplores.ordertracker.dto.OrderView;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderItem;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.exception.ResourceNotFoundException;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product flashCards;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .name("Meera Krishnan")
                .address("Chennai")
                .mobileNumber("9840012345")
                .build();

        flashCards = Product.builder()
                .id(10L)
                .name("Alphabet Flash Cards (A-Z)")
                .price(new BigDecimal("349.00"))
                .quantityInHand(10)
                .quantityToBeOrdered(0)
                .build();
    }

    /* ------------------------------------------------------------- creation */

    @Test
    @DisplayName("Creating an order deducts stock when enough quantity is in hand")
    void createOrderDeductsStock() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(flashCards));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderCreationResult result = orderService.create(orderForm(10L, 3));

        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.hasWarnings()).isFalse();
        assertThat(flashCards.getQuantityInHand()).isEqualTo(7);
        assertThat(flashCards.getQuantityToBeOrdered()).isZero();

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getOrderItems()).hasSize(1);
        assertThat(saved.getOrderItems().get(0).getUnitPrice()).isEqualByComparingTo("349.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("1047.00");
    }

    @Test
    @DisplayName("Insufficient stock still creates the order and books the shortfall to be ordered")
    void createOrderWithShortfall() {
        flashCards.setQuantityInHand(2);
        flashCards.setQuantityToBeOrdered(1);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(flashCards));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderCreationResult result = orderService.create(orderForm(10L, 5));

        assertThat(flashCards.getQuantityInHand()).isZero();
        // 1 already pending + shortfall of 3
        assertThat(flashCards.getQuantityToBeOrdered()).isEqualTo(4);
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("3 more");
    }

    @Test
    @DisplayName("Duplicate product rows are merged into a single order item")
    void duplicateRowsAreMerged() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(productRepository.findById(10L)).thenReturn(Optional.of(flashCards));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderForm form = orderForm(10L, 2);
        form.getItems().add(new OrderItemForm(10L, 3));

        orderService.create(form);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        assertThat(captor.getValue().getOrderItems()).hasSize(1);
        assertThat(captor.getValue().getOrderItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(flashCards.getQuantityInHand()).isEqualTo(5);
    }

    @Test
    @DisplayName("An order without any item is rejected")
    void createOrderWithoutItemsFails() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        OrderForm form = OrderForm.builder().customerId(1L).orderDate(LocalDate.now()).build();

        assertThatThrownBy(() -> orderService.create(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one product");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("An unknown customer is reported as not found")
    void createOrderWithUnknownCustomerFails() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        OrderForm form = orderForm(10L, 1);
        form.setCustomerId(99L);

        assertThatThrownBy(() -> orderService.create(form))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ------------------------------------------------------------- editing */

    @Test
    @DisplayName("A NEW order can be edited: old stock is given back and the new lines are deducted")
    void updateNewOrderAdjustsStock() {
        Order order = existingOrder(OrderStatus.NEW);   // 3 x flashCards, no shortfall
        flashCards.setQuantityInHand(7);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderForm form = orderForm(10L, 5);
        form.setId(100L);

        OrderCreationResult result = orderService.update(form);

        assertThat(result.getOrderId()).isEqualTo(100L);
        assertThat(result.hasWarnings()).isFalse();
        // 7 back to 10 after returning the old 3, then 5 deducted
        assertThat(flashCards.getQuantityInHand()).isEqualTo(5);
        assertThat(order.getOrderItems()).hasSize(1);
        assertThat(order.getOrderItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Editing is rejected once the order has moved past NEW")
    void updateIsOnlyAllowedWhileNew() {
        Order order = existingOrder(OrderStatus.PRINTING_IN_PROGRESS);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        OrderForm form = orderForm(10L, 1);
        form.setId(100L);

        assertThatThrownBy(() -> orderService.update(form))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only orders that are still New can be edited");
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("The edit form can only be loaded for a NEW order")
    void editFormOnlyForNewOrders() {
        Order newOrder = existingOrder(OrderStatus.NEW);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(newOrder));

        OrderForm form = orderService.getEditForm(100L);

        assertThat(form.getId()).isEqualTo(100L);
        assertThat(form.getCustomerId()).isEqualTo(1L);
        assertThat(form.isNewCustomer()).isFalse();
        assertThat(form.getItems()).hasSize(1);
        assertThat(form.getItems().get(0).getQuantity()).isEqualTo(3);

        Order packed = existingOrder(OrderStatus.PACKED);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(packed));
        assertThatThrownBy(() -> orderService.getEditForm(101L))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Editing an order that had a shortfall reverses the shortfall too")
    void updateReversesShortfall() {
        Order order = existingOrder(OrderStatus.NEW);
        order.getOrderItems().get(0).setShortfallQuantity(2);   // 1 came from stock, 2 were booked
        flashCards.setQuantityInHand(0);
        flashCards.setQuantityToBeOrdered(2);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderForm form = orderForm(10L, 1);
        form.setId(100L);

        orderService.update(form);

        // returned: +1 to hand, -2 to be ordered; then 1 deducted again
        assertThat(flashCards.getQuantityInHand()).isZero();
        assertThat(flashCards.getQuantityToBeOrdered()).isZero();
    }

    /* --------------------------------------------------------- status rules */

    @Test
    @DisplayName("Status moves forward one step at a time")
    void statusMovesForward() {
        Order order = existingOrder(OrderStatus.NEW);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderView view = orderService.updateStatus(100L, OrderStatus.PRINTING_IN_PROGRESS, null);

        assertThat(view.getOrderStatus()).isEqualTo(OrderStatus.PRINTING_IN_PROGRESS);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PRINTING_IN_PROGRESS);
    }

    @Test
    @DisplayName("Skipping a status is not allowed")
    void cannotSkipStatus() {
        Order order = existingOrder(OrderStatus.NEW);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, OrderStatus.SHIPPED, "TRK1"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only move forward");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    @DisplayName("Going backwards is not allowed")
    void cannotMoveBackwards() {
        Order order = existingOrder(OrderStatus.PACKED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, OrderStatus.NEW, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("only move forward");
    }

    @Test
    @DisplayName("Setting the same status again is rejected")
    void cannotSetSameStatus() {
        Order order = existingOrder(OrderStatus.PACKED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, OrderStatus.PACKED, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already");
    }

    @Test
    @DisplayName("Shipping without a tracking id is rejected")
    void shippingRequiresTrackingId() {
        Order order = existingOrder(OrderStatus.PACKED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, OrderStatus.SHIPPED, "  "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tracking id is required");
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PACKED);
    }

    @Test
    @DisplayName("Shipping with a tracking id stores the tracking id")
    void shippingStoresTrackingId() {
        Order order = existingOrder(OrderStatus.PACKED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        OrderView view = orderService.updateStatus(100L, OrderStatus.SHIPPED, " DTDC7781234567 ");

        assertThat(view.getOrderStatus()).isEqualTo(OrderStatus.SHIPPED);
        assertThat(order.getTrackingId()).isEqualTo("DTDC7781234567");
    }

    @Test
    @DisplayName("Delivered is a final status")
    void deliveredIsFinal() {
        Order order = existingOrder(OrderStatus.DELIVERED);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(100L, OrderStatus.SHIPPED, "TRK1"))
                .isInstanceOf(BusinessRuleException.class);
    }

    /* ---------------------------------------------------------- delete rules */

    @Test
    @DisplayName("Deleting an order restores the stock")
    void deleteRestoresStock() {
        Order order = existingOrder(OrderStatus.NEW);
        flashCards.setQuantityInHand(1);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));

        orderService.delete(100L);

        assertThat(flashCards.getQuantityInHand()).isEqualTo(4); // 1 + 3 from the order
        verify(productRepository).save(flashCards);
        verify(orderRepository).delete(order);
    }

    /* ------------------------------------------------------------- helpers */

    private OrderForm orderForm(Long productId, int quantity) {
        OrderForm form = new OrderForm();
        form.setCustomerId(1L);
        form.setOrderDate(LocalDate.now());
        form.setCustomizationNotes("Print the name Aarav");
        form.getItems().add(new OrderItemForm(productId, quantity));
        return form;
    }

    @Test
    @DisplayName("Delivering an order removes the printed shortfall units from stock")
    void deliveryDeductsShortfallUnits() {
        Order order = existingOrder(OrderStatus.SHIPPED);
        order.getOrderItems().get(0).setShortfallQuantity(2);
        flashCards.setQuantityInHand(5);
        flashCards.setQuantityToBeOrdered(2);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(100L, OrderStatus.DELIVERED, null);

        assertThat(flashCards.getQuantityInHand()).isEqualTo(3);
        assertThat(flashCards.getQuantityToBeOrdered()).isZero();
        assertThat(order.getOrderItems().get(0).getShortfallQuantityOrZero()).isZero();
    }

    @Test
    @DisplayName("Delivering an order that had no shortfall leaves stock untouched")
    void deliveryWithoutShortfallKeepsStock() {
        Order order = existingOrder(OrderStatus.SHIPPED);
        order.getOrderItems().get(0).setShortfallQuantity(0);
        flashCards.setQuantityInHand(5);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        orderService.updateStatus(100L, OrderStatus.DELIVERED, null);

        assertThat(flashCards.getQuantityInHand()).isEqualTo(5);
    }

    private Order existingOrder(OrderStatus status) {
        Order order = Order.builder()
                .id(100L)
                .customer(customer)
                .orderDate(LocalDate.now())
                .orderStatus(status)
                .build();
        OrderItem item = OrderItem.builder()
                .id(1L)
                .product(flashCards)
                .quantity(3)
                .unitPrice(flashCards.getPrice())
                .build();
        order.addItem(item);
        lenient().when(productRepository.findById(10L)).thenReturn(Optional.of(flashCards));
        return order;
    }
}

