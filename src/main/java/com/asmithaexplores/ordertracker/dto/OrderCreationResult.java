package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Result of creating an order: the new id plus any stock shortfall warnings. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCreationResult {

    private Long orderId;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}

