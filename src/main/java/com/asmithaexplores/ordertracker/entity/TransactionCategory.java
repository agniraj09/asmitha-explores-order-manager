package com.asmithaexplores.ordertracker.entity;

import java.util.Arrays;
import java.util.List;

/**
 * What the money was for. Every category belongs to exactly one {@link TransactionType}, so the
 * type of a transaction is always derived from its category and the two can never disagree.
 */
public enum TransactionCategory {

    ORDER_SALE("Order sale", TransactionType.INCOME),
    OTHER_INCOME("Other income", TransactionType.INCOME),

    PRINTING("Printing", TransactionType.EXPENSE),
    PACKING("Packing", TransactionType.EXPENSE),
    SHIPPING("Shipping charges", TransactionType.EXPENSE),
    TRANSPORT("Transport", TransactionType.EXPENSE),
    MATERIALS("Materials", TransactionType.EXPENSE),
    OTHER_EXPENSE("Other expense", TransactionType.EXPENSE);

    private final String label;
    private final TransactionType type;

    TransactionCategory(String label, TransactionType type) {
        this.label = label;
        this.type = type;
    }

    public String getLabel() {
        return label;
    }

    public TransactionType getType() {
        return type;
    }

    /** The categories that may be picked for the given direction of money. */
    public static List<TransactionCategory> of(TransactionType type) {
        return Arrays.stream(values()).filter(c -> c.type == type).toList();
    }
}
