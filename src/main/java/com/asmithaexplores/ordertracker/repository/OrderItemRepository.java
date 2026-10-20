package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    boolean existsByProductId(Long productId);

    /**
     * Units of each product committed to orders that are still {@code NEW}, i.e. nothing has
     * been printed or packed for them yet. Returns rows of [productId, units].
     */
    @Query("""
            select i.product.id, sum(i.quantity)
            from OrderItem i
            where i.order.orderStatus = com.asmithaexplores.ordertracker.entity.OrderStatus.NEW
            group by i.product.id
            """)
    List<Object[]> sumUnitsInNewOrders();
}
