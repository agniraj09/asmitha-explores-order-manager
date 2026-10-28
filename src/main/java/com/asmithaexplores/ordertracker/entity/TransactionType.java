package com.asmithaexplores.ordertracker.entity;

/**
 * Direction of money for a {@link Transaction}: what came in from orders versus what was
 * spent on running the shop.
 */
public enum TransactionType {

    INCOME("Income", "success", "bi-arrow-down-left"),
    EXPENSE("Expense", "danger", "bi-arrow-up-right");

    private final String label;
    private final String badgeClass;
    private final String icon;

    TransactionType(String label, String badgeClass, String icon) {
        this.label = label;
        this.badgeClass = badgeClass;
        this.icon = icon;
    }

    public String getLabel() {
        return label;
    }

    public String getBadgeClass() {
        return badgeClass;
    }

    public String getIcon() {
        return icon;
    }
}
