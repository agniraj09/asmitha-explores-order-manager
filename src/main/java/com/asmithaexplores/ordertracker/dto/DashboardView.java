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

    /** Products blocking open orders / already booked to be printed. */
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
    private String currentMonthLabel;
}
