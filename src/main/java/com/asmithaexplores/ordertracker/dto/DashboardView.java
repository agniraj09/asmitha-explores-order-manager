package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardView {

    @Builder.Default
    private Map<OrderStatus, Long> ordersPerStatus = new LinkedHashMap<>();
    @Builder.Default
    private List<OrderView> pendingOrders = new ArrayList<>();

    /** Products with units still booked to be printed/ordered. */
    @Builder.Default
    private List<RestockView> restockProducts = new ArrayList<>();

    /** Products that have run out completely. */
    @Builder.Default
    private List<ProductView> outOfStockProducts = new ArrayList<>();
    private long totalOrdersThisMonth;
    private long totalOrders;
    private long totalCustomers;
    @Builder.Default
    private BigDecimal revenueThisMonth = BigDecimal.ZERO;

    /** Money actually booked in the ledger this month - see the Finance screen. */
    @Builder.Default
    private BigDecimal incomeThisMonth = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal expensesThisMonth = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal profitThisMonth = BigDecimal.ZERO;

    private String currentMonthLabel;

    public boolean isProfitableThisMonth() {
        return profitThisMonth.signum() >= 0;
    }
}
