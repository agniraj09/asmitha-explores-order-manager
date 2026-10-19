package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerView {

    private Long id;
    private String name;
    private String address;
    private String mobileNumber;
    private String alternateMobileNumber;
    /** Derived from the customer's orders - never stored. */
    private long numberOfOrders;
}

