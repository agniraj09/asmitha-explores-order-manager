package com.asmithaexplores.ordertracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * One printable shipping label, built from an order's customer. Everything the browser needs to
 * draw and export the label is here, so the label page never has to reach back to the server.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressLabelView {

    private Long orderId;
    private String customerName;
    private String statusLabel;
    private String statusBadgeClass;
    private String orderDateLabel;

    /** Address split into the lines it was entered with. */
    @Builder.Default
    private List<String> addressLines = new ArrayList<>();

    /** Primary number first, then the alternate one when it is a different number. */
    @Builder.Default
    private List<String> phones = new ArrayList<>();

    /** Whether the label page ticks this order on load. */
    private boolean selectedByDefault;
}
