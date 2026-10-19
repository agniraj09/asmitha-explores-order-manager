package com.asmithaexplores.ordertracker.entity;

/**
 * Lifecycle of an order. The flow is forward-only, in declaration order.
 */
public enum OrderStatus {

    NEW("New", "secondary"),
    PRINTING_IN_PROGRESS("Printing in progress", "info"),
    PACKED("Packed", "primary"),
    SHIPPED("Shipped", "warning"),
    DELIVERED("Delivered", "success");

    private final String label;
    private final String badgeClass;

    OrderStatus(String label, String badgeClass) {
        this.label = label;
        this.badgeClass = badgeClass;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    /** The only status this status may move to, or {@code null} when it is final. */
    public OrderStatus next() {
        OrderStatus[] values = values();
        return ordinal() == values.length - 1 ? null : values[ordinal() + 1];
    }

    public boolean canMoveTo(OrderStatus target) {
        return target != null && target.ordinal() == ordinal() + 1;
    }
}

