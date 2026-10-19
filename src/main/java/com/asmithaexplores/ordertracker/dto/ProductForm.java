package com.asmithaexplores.ordertracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductForm {

    private Long id;

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Name cannot be longer than 150 characters")
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 40, message = "Category cannot be longer than 40 characters")
    private String category;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Price can have at most 2 decimals")
    private BigDecimal price;

    @NotNull(message = "Quantity in hand is required")
    @Min(value = 0, message = "Quantity in hand cannot be negative")
    private Integer quantityInHand;

    @NotNull(message = "Quantity to be ordered is required")
    @Min(value = 0, message = "Quantity to be ordered cannot be negative")
    private Integer quantityToBeOrdered;
}

