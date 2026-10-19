package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A product that needs attention on the dashboard: stock in hand cannot cover the
 * open (not delivered) orders that still need it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestockView {

    private Long id;
    private String name;
    private String category;
    private int quantityInHand;
    private int quantityToBeOrdered;

    /** Units committed to orders that are not delivered yet. */
    private int unitsInOpenOrders;

    private boolean outOfStock;

    /** How many units are still missing to satisfy every open order. */
    public int getShortfall() {
        return Math.max(0, unitsInOpenOrders - quantityInHand);
    }

    /**
     * Units to print/order now: whatever was already booked as "to be ordered",
     * or the gap between the open orders and the stock in hand - whichever is bigger.
     */
    public int getRequiredQuantity() {
        return Math.max(getShortfall(), quantityToBeOrdered);
    }

    /** Stock cannot cover the open orders - the owner has to print/order more. */
    public boolean isBlocking() {
        return getRequiredQuantity() > 0;
    }

    /** Only products that have run out are highlighted. */
    public String getRowClass() {
        return outOfStock ? "table-danger" : "";
    }
}

