package com.asmithaexplores.ordertracker.repository;

import com.asmithaexplores.ordertracker.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    /** The automatic income line of an order, if it still exists. */
    Optional<Transaction> findByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);

    boolean existsByTagsId(Long tagId);

    /**
     * Totals per direction of money inside a date window.
     *
     * @return rows of {@code [TransactionType, sum(amount)]}
     */
    @Query("""
            select t.type, coalesce(sum(t.amount), 0)
            from Transaction t
            where t.transactionDate between :from and :to
            group by t.type
            """)
    List<Object[]> sumByTypeBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
