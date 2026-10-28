package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.TransactionCategory;
import com.asmithaexplores.ordertracker.entity.TransactionType;
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
public class TransactionFilter {

    private TransactionType type;
    private TransactionCategory category;
    private Long tagId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    public boolean isEmpty() {
        return type == null && category == null && tagId == null && fromDate == null && toDate == null;
    }
}
