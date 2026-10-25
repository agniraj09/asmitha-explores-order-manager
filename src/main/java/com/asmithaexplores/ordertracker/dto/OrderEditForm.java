package com.asmithaexplores.ordertracker.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderEditForm {

    private Long id;

    @Size(max = 2000, message = "Notes cannot be longer than 2000 characters")
    private String customizationNotes;

    @Size(max = 2000, message = "Special notes cannot be longer than 2000 characters")
    private String specialNotes;

    @Size(max = 60, message = "Tracking id cannot be longer than 60 characters")
    private String trackingId;
}

