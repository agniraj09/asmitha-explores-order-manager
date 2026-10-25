package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findByCustomerIdOrderByOrderDateDescIdDesc(Long customerId);

    long countByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);

    /** Orders per status in a single round trip. Returns rows of [status, count]. */
    @Query("select o.orderStatus, count(o) from Order o group by o.orderStatus")
    List<Object[]> countGroupedByStatus();

    /**
     * Number and total value of the orders placed in the given window, in one round trip.
     * Returns a single row of [orderCount, orderValue]; the value is {@code null} when the
     * window holds no order lines at all.
     */
    @Query("""
            select count(distinct o.id), sum(i.unitPrice * i.quantity)
            from Order o left join o.orderItems i
            where o.orderDate between :from and :to
            """)
    List<Object[]> summariseOrdersBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Everything that is not delivered yet, with the customer and the items (and their product)
     * loaded in the same query - these lists always render all of that, and fetching them lazily
     * costs one extra round trip per order.
     */
    @EntityGraph(attributePaths = {"customer", "orderItems", "orderItems.product"})
    List<Order> findByOrderStatusNotOrderByOrderDateAscIdAsc(OrderStatus status);

    /** Candidates for the delivery sync: on the road, with something to look up. */
    List<Order> findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus status);

    @Query("select o.customer.id, count(o) from Order o group by o.customer.id")
    List<Object[]> countGroupedByCustomer();
}
