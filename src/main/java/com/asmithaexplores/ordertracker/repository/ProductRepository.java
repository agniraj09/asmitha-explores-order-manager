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
     * Products that still owe units to open orders: the shortfall booked when stock could not
     * cover an order ({@code quantityToBeOrdered > 0}). This ledger is maintained by order
     * creation/deletion and cleared by the dashboard "Ordered" action, so it is the single
     * source of truth for what has to be printed. Comparing open-order quantities against stock
     * in hand would double-count the units already deducted when the order was placed.
     */
    @Query("""
            select p from Product p
            where p.quantityToBeOrdered > 0
            order by p.quantityToBeOrdered desc, p.quantityInHand asc, p.name asc
            """)
    List<Product> findNeedingRestock();
}
