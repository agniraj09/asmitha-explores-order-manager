package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Totals for whatever set of transactions is currently on screen: what came in, what went out,
 * what is left, and the same split by expense category and by tag.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionSummary {

    @Builder.Default
    private BigDecimal totalIncome = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private long transactionCount;

    /** Expense total per category label, biggest first. */
    @Builder.Default
    private Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();

    /** Expense total per tag name, biggest first. Untagged expenses are grouped as "Untagged". */
    @Builder.Default
    private Map<String, BigDecimal> expenseByTag = new LinkedHashMap<>();

    public BigDecimal getNet() {
        return totalIncome.subtract(totalExpense);
    }

    public boolean isProfitable() {
        return getNet().signum() >= 0;
    }
}
