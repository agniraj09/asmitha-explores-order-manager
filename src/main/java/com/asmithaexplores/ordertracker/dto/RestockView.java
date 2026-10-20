package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A product that needs attention on the dashboard: a shortfall was booked because stock
 * could not cover an order that is not delivered yet.
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

    /** Units committed to orders that are still new - shown for context only. */
    private int unitsInNewOrders;

    private boolean outOfStock;

    /**
     * Units to print/order now. This is the booked shortfall: the units that were never taken
     * out of stock because there were none left. Order quantities cannot be used here -
     * whatever stock could cover was already deducted when the order was placed.
     */
    public int getRequiredQuantity() {
        return Math.max(0, quantityToBeOrdered);
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

