package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Everything printed on one bill copy. Built once from an order plus the configured seller
 * details, so the PDF template only has to lay the values out.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillView {

    private Long orderId;

    /** Human readable bill number, e.g. {@code AE-000017}. */
    private String billNumber;

    /** Date the order was placed, formatted for printing. */
    private String billDate;

    private String trackingId;

    /* ------------------------------------------------------------------ from */

    private String sellerName;

    @Builder.Default
    private List<String> sellerAddressLines = new ArrayList<>();

    private String sellerMobile;
    private String sellerEmail;
    private String sellerGstin;

    /* -------------------------------------------------------------------- to */

    private String buyerName;

    @Builder.Default
    private List<String> buyerAddressLines = new ArrayList<>();

    private String buyerMobile;

    /* ----------------------------------------------------------------- items */

    @Builder.Default
    private List<OrderItemView> items = new ArrayList<>();

    private int totalQuantity;
    private BigDecimal totalAmount;

    /** Total spelled out in words, so the amount cannot be altered after printing. */
    private String totalInWords;

    private String notes;
    private String footerNote;

    /** File name offered to the browser when the bill is downloaded. */
    public String getFileName() {
        return "Bill_" + (billNumber == null ? orderId : billNumber) + ".pdf";
    }
}
