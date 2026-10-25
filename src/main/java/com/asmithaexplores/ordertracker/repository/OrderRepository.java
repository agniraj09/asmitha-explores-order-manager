package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    List<Order> findByCustomerIdOrderByOrderDateDescIdDesc(Long customerId);

    long countByCustomerId(Long customerId);

    boolean existsByCustomerId(Long customerId);

    long countByOrderStatus(OrderStatus status);

    long countByOrderDateBetween(LocalDate from, LocalDate to);

    List<Order> findByOrderStatusNotOrderByOrderDateAscIdAsc(OrderStatus status);

    /** Candidates for the delivery sync: on the road, with something to look up. */
    List<Order> findByOrderStatusAndTrackingIdIsNotNullOrderByOrderDateAscIdAsc(OrderStatus status);

    @Query("select o.customer.id, count(o) from Order o group by o.customer.id")
    List<Object[]> countGroupedByCustomer();
}
