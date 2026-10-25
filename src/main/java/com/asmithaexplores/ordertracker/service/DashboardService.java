package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.DashboardView;
import com.asmithaexplores.ordertracker.dto.OrderPeriodSummary;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    private final OrderService orderService;
    private final ProductService productService;
    private final CustomerService customerService;

    public DashboardView build() {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());

        Map<OrderStatus, Long> ordersPerStatus = orderService.countPerStatus();
        OrderPeriodSummary thisMonth = orderService.summariseBetween(firstDayOfMonth, lastDayOfMonth);

        return DashboardView.builder()
                .ordersPerStatus(ordersPerStatus)
                .pendingOrders(orderService.findPendingOrders())
                .restockProducts(productService.findNeedingRestock())
                .outOfStockProducts(productService.findOutOfStock())
                .totalOrdersThisMonth(thisMonth.getOrderCount())
                .totalOrders(ordersPerStatus.values().stream().mapToLong(Long::longValue).sum())
                .totalCustomers(customerService.count())
                .revenueThisMonth(thisMonth.getOrderValue())
                .currentMonthLabel(today.format(MONTH_FORMAT))
                .build();
    }
}

