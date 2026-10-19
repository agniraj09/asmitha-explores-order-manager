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
public class ProductView {

    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private int quantityInHand;
    private int quantityToBeOrdered;
    private boolean outOfStock;

    /** Only products that have run out are highlighted. */
    public String getRowClass() {
        return outOfStock ? "table-danger" : "";
    }
}

