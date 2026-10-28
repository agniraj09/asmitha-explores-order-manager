package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.TransactionCategory;
import com.asmithaexplores.ordertracker.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionView {

    private Long id;
    private TransactionType type;
    private TransactionCategory category;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String formattedDate;
    private String description;

    /** Set for the automatic income line of an order. */
    private Long orderId;
    private String customerName;

    private boolean systemGenerated;
    private boolean manuallyAdjusted;

    @Builder.Default
    private List<TransactionTagView> tags = new ArrayList<>();

    public boolean isIncome() {
        return type == TransactionType.INCOME;
    }

    /** Positive for income, negative for an expense - used for the running figures in the UI. */
    public BigDecimal getSignedAmount() {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return isIncome() ? value : value.negate();
    }

    /** Income lines of an order may not be deleted by hand - the order owns them. */
    public boolean isDeletable() {
        return orderId == null;
    }
}
