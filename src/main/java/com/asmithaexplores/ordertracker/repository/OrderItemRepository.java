package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);

    /**
     * Units of each product committed to orders that are not delivered yet,
     * i.e. what still has to be printed/packed. Returns rows of [productId, units].
     */
    @Query("""
            select i.product.id, sum(i.quantity)
            from OrderItem i
            where i.order.orderStatus <> com.asmithaexplores.ordertracker.entity.OrderStatus.DELIVERED
            group by i.product.id
            """)
    List<Object[]> sumUnitsInOpenOrders();

    /** Units of one product committed to orders that are not delivered yet. */
    @Query("""
            select coalesce(sum(i.quantity), 0)
            from OrderItem i
            where i.product.id = :productId
              and i.order.orderStatus <> com.asmithaexplores.ordertracker.entity.OrderStatus.DELIVERED
            """)
    int sumUnitsInOpenOrdersForProduct(@Param("productId") Long productId);
}
