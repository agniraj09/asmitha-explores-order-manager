package com.asmithaexplores.ordertracker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderForm {

    /** Null when creating, set when editing an order that is still NEW. */
    private Long id;

    /** Existing customer. Ignored when {@link #newCustomer} is true. */
    private Long customerId;

    /**
     * When true the customer details below are used to create a brand new customer.
     * Defaults to true because most orders come from first-time customers.
     */
    @Builder.Default
    private boolean newCustomer = true;

    private String newCustomerName;
    private String newCustomerAddress;
    private String newCustomerMobileNumber;

    @Builder.Default
    @Valid
    @NotEmpty(message = "Add at least one product to the order")
    private List<OrderItemForm> items = new ArrayList<>();

    @Size(max = 2000, message = "Notes cannot be longer than 2000 characters")
    private String customizationNotes;

    @Size(max = 2000, message = "Special notes cannot be longer than 2000 characters")
    private String specialNotes;

    @NotNull(message = "Order date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Builder.Default
    private LocalDate orderDate = LocalDate.now();
}

