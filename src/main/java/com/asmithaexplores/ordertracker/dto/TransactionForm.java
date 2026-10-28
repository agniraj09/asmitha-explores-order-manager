package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.TransactionCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionForm {

    private Long id;

    @NotNull(message = "Choose what the money was for")
    private TransactionCategory category;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Amount can have at most 2 decimals")
    private BigDecimal amount;

    @NotNull(message = "Date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate transactionDate;

    @Size(max = 500, message = "Description cannot be longer than 500 characters")
    private String description;

    @Builder.Default
    private List<Long> tagIds = new ArrayList<>();

    /** Read-only context shown on the form when editing the income line of an order. */
    private Long orderId;
}
