package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemView {

    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    /** Units of {@link #quantity} that were booked as "to be ordered" when the order was placed. */
    private int shortfallQuantity;

    /**
     * Owed units that are still not on the shelf, i.e. what the dashboard "Ordered" button has to
     * produce before the order can be packed. Zero once they have been printed into stock.
     */
    private int pendingPrintQuantity;
}

