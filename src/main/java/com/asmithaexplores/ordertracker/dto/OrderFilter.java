package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderFilter {

    private OrderStatus status;
    private Long customerId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    public boolean isEmpty() {
        return status == null && customerId == null && fromDate == null && toDate == null;
    }
}

