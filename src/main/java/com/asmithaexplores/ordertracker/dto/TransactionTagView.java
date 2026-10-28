package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTagView {

    private Long id;
    private String name;
    private String colorHex;
    private String description;

    /** {@code true} when the tag is still referenced by at least one transaction. */
    private boolean inUse;
}
