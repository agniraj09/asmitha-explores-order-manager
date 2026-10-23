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

    /** Token standing in for the courier tracking url in {@link #getTrackingMessageTemplate()}. */
    public static final String TRACKING_URL_PLACEHOLDER = "{{trackingUrl}}";

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

    /** Units of this order that still have to be printed before it can be packed. */
    public int getUnitsToPrint() {
        return items.stream().mapToInt(OrderItemView::getPendingPrintQuantity).sum();
    }

    /**
     * The order cannot move to {@link OrderStatus#PACKED} yet: stock does not hold the units it
     * owes. Pressing "Ordered" on the dashboard puts them in stock and clears this.
     */
    public boolean isWaitingForPrint() {
        return orderStatus != null
                && orderStatus.ordinal() < OrderStatus.PACKED.ordinal()
                && getUnitsToPrint() > 0;
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

    /** Ready-to-paste WhatsApp message for a shipped order, without a courier tracking link. */
    public String getTrackingMessage() {
        return trackingMessage(null);
    }

    /**
     * The same message with the courier link left as {@value #TRACKING_URL_PLACEHOLDER}, so the
     * browser can swap in a tracking url typed by the owner without duplicating the wording here.
     * Dropping the whole line removes the tracking link from the message.
     */
    public String getTrackingMessageTemplate() {
        return trackingMessage(TRACKING_URL_PLACEHOLDER);
    }

    /**
     * Ready-to-paste WhatsApp message for a shipped order.
     * <p>
     * Only characters from the basic multilingual plane are used. Emoji such as 👋 sit above it
     * and are carried as a surrogate pair, which does not survive the hand-off to WhatsApp - the
     * customer receives a replacement character. {@code \u263A} plus the variation selector
     * {@code \uFE0F} renders as an emoji just the same and is encoded like the rupee sign, which
     * does come through intact.
     *
     * @param trackingUrl courier tracking page to point the customer at; the line is left out
     *                    entirely when blank
     */
    public String trackingMessage(String trackingUrl) {
        StringBuilder message = new StringBuilder()
                .append("Hi ").append(customerName).append(",\n")
                .append("Your Asmitha Explores order #").append(id).append(" has been shipped.\n")
                .append("Tracking ID: ").append(trackingId == null ? "" : trackingId).append('\n');
        if (trackingUrl != null && !trackingUrl.isBlank()) {
            message.append("Track here: ").append(trackingUrl.trim()).append('\n');
        }
        return message
                .append("Items: ").append(getItemsSummary()).append('\n')
                .append("Total: \u20B9").append(totalAmount == null ? "0" : totalAmount.toPlainString()).append('\n')
                .append("Thank you for shopping with us! \u263A\uFE0F")
                .toString();
    }

    /** Number the tracking message is sent to, in the international format WhatsApp expects. */
    public String getWhatsAppNumber() {
        return customerMobileNumber == null ? "" : "91" + customerMobileNumber.trim();
    }
}

