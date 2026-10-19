package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.DashboardView;
import com.asmithaexplores.ordertracker.dto.OrderFilter;
import com.asmithaexplores.ordertracker.dto.OrderView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

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

        List<OrderView> ordersThisMonth = orderService.search(
                OrderFilter.builder().fromDate(firstDayOfMonth).toDate(lastDayOfMonth).build());

        BigDecimal revenue = ordersThisMonth.stream()
                .map(OrderView::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardView.builder()
                .ordersPerStatus(orderService.countPerStatus())
                .pendingOrders(orderService.findPendingOrders())
                .restockProducts(productService.findNeedingRestock())
                .outOfStockProducts(productService.findOutOfStock())
                .totalOrdersThisMonth(ordersThisMonth.size())
                .totalOrders(orderService.count())
                .totalCustomers(customerService.count())
                .revenueThisMonth(revenue)
                .currentMonthLabel(today.format(MONTH_FORMAT))
                .build();
    }
}

