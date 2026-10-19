package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByNameAsc();

    List<Product> findAllByOrderByCategoryAscNameAsc();

    @Query("select distinct p.category from Product p where p.category is not null order by p.category asc")
    List<String> findDistinctCategories();

    @Query("select p from Product p where p.quantityInHand <= 0 order by p.name asc")
    List<Product> findOutOfStock();

    /**
     * Products that actually need printing/ordering right now: the units committed to orders that
     * are not delivered yet exceed the stock in hand. Everything else - including products with a
     * manually booked "to be ordered" quantity that stock already covers - stays on the Inventory
     * screen, so the dashboard reads as a to-do list.
     */
    @Query("""
            select p from Product p
            where (select coalesce(sum(i.quantity), 0) from OrderItem i
                   where i.product = p
                     and i.order.orderStatus <> com.asmithaexplores.ordertracker.entity.OrderStatus.DELIVERED)
                  > p.quantityInHand
            order by p.quantityToBeOrdered desc, p.quantityInHand asc, p.name asc
            """)
    List<Product> findNeedingRestock();
}
