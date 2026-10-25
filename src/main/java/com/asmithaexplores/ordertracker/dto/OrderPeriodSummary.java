package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** How many orders were placed in a period and what they were worth. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderPeriodSummary {

    private long orderCount;
    private BigDecimal orderValue = BigDecimal.ZERO;
}
