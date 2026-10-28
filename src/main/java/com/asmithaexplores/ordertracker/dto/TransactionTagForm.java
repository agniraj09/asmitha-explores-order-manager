package com.asmithaexplores.ordertracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTagForm {

    private Long id;

    @NotBlank(message = "Tag name is required")
    @Size(max = 60, message = "Tag name cannot be longer than 60 characters")
    private String name;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Colour must look like #a1b2c3")
    private String colorHex;

    @Size(max = 200, message = "Description cannot be longer than 200 characters")
    private String description;
}
