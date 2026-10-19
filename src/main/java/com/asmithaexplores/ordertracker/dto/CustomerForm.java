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
public class CustomerForm {

    private Long id;

    @NotBlank(message = "Customer name is required")
    @Size(max = 120, message = "Name cannot be longer than 120 characters")
    private String name;

    @NotBlank(message = "Address is required")
    @Size(max = 1000, message = "Address cannot be longer than 1000 characters")
    private String address;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$",
            message = "Enter a valid 10-digit Indian mobile number (starting with 6, 7, 8 or 9)")
    private String mobileNumber;
}

