package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.BillView;
import com.asmithaexplores.ordertracker.service.BillService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;

/** Downloadable bill copy of an order. */
@Controller
@RequestMapping("/orders/{orderId}/bill")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    /**
     * The bill as a PDF.
     *
     * @param inline {@code true} opens the bill in the browser's PDF viewer instead of saving it,
     *               which is what the "preview" link on the order page uses
     */
    @GetMapping(produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable Long orderId,
                                           @RequestParam(defaultValue = "false") boolean inline) {
        BillView bill = billService.getBill(orderId);
        byte[] pdf = billService.renderPdf(bill);

        ContentDisposition disposition = (inline
                ? ContentDisposition.inline()
                : ContentDisposition.attachment())
                .filename(bill.getFileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
