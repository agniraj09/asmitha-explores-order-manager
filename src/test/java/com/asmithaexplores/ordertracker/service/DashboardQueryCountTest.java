package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.DashboardView;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderItem;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the dashboard against the "one query per order" regression: it used to issue a
 * separate select for every order's items (and for every status counter), which made the
 * page take seconds against a hosted database. The number of statements must stay flat no
 * matter how many orders exist.
 */
@SpringBootTest
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class DashboardQueryCountTest {

    private static final int CUSTOMERS = 40;
    private static final int ORDERS_PER_CUSTOMER = 3;
    private static final int ITEMS_PER_ORDER = 3;
    private static final int QUANTITY_PER_ITEM = 2;
    private static final BigDecimal UNIT_PRICE = new BigDecimal("100.00");

    /** Enough headroom for the handful of aggregate queries, far below one-per-order. */
    private static final int MAX_STATEMENTS = 8;

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void seed() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();

        for (int c = 0; c < CUSTOMERS; c++) {
            Customer customer = customerRepository.save(Customer.builder()
                    .name("Customer " + c)
                    .mobileNumber("9" + String.format("%09d", c))
                    .address("Line 1\nLine 2")
                    .build());
            Product product = productRepository.save(Product.builder()
                    .name("Product " + c)
                    .category("Cat")
                    .price(UNIT_PRICE)
                    .quantityInHand(10)
                    .quantityToBeOrdered(0)
                    .build());
            for (int o = 0; o < ORDERS_PER_CUSTOMER; o++) {
                Order order = Order.builder()
                        .customer(customer)
                        .orderDate(LocalDate.now())
                        .orderStatus(OrderStatus.NEW)
                        .build();
                for (int i = 0; i < ITEMS_PER_ORDER; i++) {
                    order.addItem(OrderItem.builder()
                            .product(product)
                            .quantity(QUANTITY_PER_ITEM)
                            .unitPrice(UNIT_PRICE)
                            .build());
                }
                orderRepository.save(order);
            }
        }
    }

    @Test
    @DisplayName("the dashboard is built with a flat, small number of queries")
    void dashboardDoesNotQueryPerOrder() {
        Statistics statistics = statistics();
        statistics.clear();

        dashboardService.build();

        assertThat(statistics.getPrepareStatementCount())
                .as("SQL statements for one dashboard render")
                .isLessThanOrEqualTo(MAX_STATEMENTS);
    }

    @Test
    @DisplayName("the order list is built with a flat, small number of queries")
    void orderListDoesNotQueryPerOrder() {
        Statistics statistics = statistics();
        statistics.clear();

        orderService.search(null);

        assertThat(statistics.getPrepareStatementCount())
                .as("SQL statements for one order list render")
                .isLessThanOrEqualTo(MAX_STATEMENTS);
    }

    @Test
    @DisplayName("the dashboard totals are still correct")
    void dashboardTotalsAreCorrect() {
        int totalOrders = CUSTOMERS * ORDERS_PER_CUSTOMER;
        BigDecimal expectedRevenue = UNIT_PRICE
                .multiply(BigDecimal.valueOf((long) totalOrders * ITEMS_PER_ORDER * QUANTITY_PER_ITEM));

        DashboardView view = dashboardService.build();

        assertThat(view.getTotalOrders()).isEqualTo(totalOrders);
        assertThat(view.getTotalOrdersThisMonth()).isEqualTo(totalOrders);
        assertThat(view.getTotalCustomers()).isEqualTo(CUSTOMERS);
        assertThat(view.getRevenueThisMonth()).isEqualByComparingTo(expectedRevenue);
        assertThat(view.getPendingOrders()).hasSize(totalOrders);
        assertThat(view.getOrdersPerStatus().get(OrderStatus.NEW)).isEqualTo(totalOrders);
        assertThat(view.getOrdersPerStatus().get(OrderStatus.DELIVERED)).isZero();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }
}
