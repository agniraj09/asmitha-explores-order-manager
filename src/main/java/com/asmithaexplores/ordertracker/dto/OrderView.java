package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderView {

    private Long id;
    private Long customerId;
    private String customerName;
    private String customerMobileNumber;
    private String customerAddress;
    private LocalDate orderDate;
    private OrderStatus orderStatus;
    private String trackingId;
    private String customizationNotes;
    @Builder.Default
    private List<OrderItemView> items = new ArrayList<>();
    private BigDecimal totalAmount;
    private int totalQuantity;

    public OrderStatus getNextStatus() {
        return orderStatus == null ? null : orderStatus.next();
    }

    public boolean isShipped() {
        return orderStatus == OrderStatus.SHIPPED || orderStatus == OrderStatus.DELIVERED;
    }

    /** Only orders that have not started printing yet can be edited. */
    public boolean isEditable() {
        return orderStatus == OrderStatus.NEW;
    }

    public boolean isDelivered() {
        return orderStatus == OrderStatus.DELIVERED;
    }

    /** Whole days since the order was placed (0 = placed today). */
    public long getDaysOpen() {
        if (orderDate == null) {
            return 0;
        }
        return Math.max(0, ChronoUnit.DAYS.between(orderDate, LocalDate.now()));
    }

    /** "Today", "1 day", "6 days" ... for the dashboard ageing column. */
    public String getDaysOpenLabel() {
        long days = getDaysOpen();
        if (days == 0) {
            return "Today";
        }
        return days + (days == 1 ? " day" : " days");
    }

    /** Orders waiting a week or more are highlighted more strongly. */
    public boolean isAging() {
        return !isDelivered() && getDaysOpen() >= 7;
    }

    public String getItemsSummary() {
        return items.stream()
                .map(i -> i.getProductName() + " x" + i.getQuantity())
                .reduce((a, b) -> a + ", " + b)
                .orElse("-");
    }

    /** Ready-to-paste WhatsApp message for a shipped order. */
    public String getTrackingMessage() {
        return "Hi " + customerName + ", \uD83D\uDC4B\n"
                + "Your Asmitha Explores order #" + id + " has been shipped.\n"
                + "Tracking ID: " + (trackingId == null ? "" : trackingId) + "\n"
                + "Items: " + getItemsSummary() + "\n"
                + "Total: \u20B9" + (totalAmount == null ? "0" : totalAmount.toPlainString()) + "\n"
                + "Thank you for shopping with us! \uD83D\uDE0A";
    }
}

