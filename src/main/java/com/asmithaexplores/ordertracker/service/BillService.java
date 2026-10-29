package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.asmithaexplores.ordertracker.dto.BillView;
import com.asmithaexplores.ordertracker.dto.OrderView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Builds the printable bill copy of an order and renders it as a PDF.
 * <p>
 * The bill is laid out as an ordinary Thymeleaf template, which Flying Saucer then paints onto an
 * A4 page. Only characters covered by the PDF base fonts are used, so no font has to be embedded:
 * the rupee sign is written as "Rs.".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BillService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private static final String[] UNITS = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen",
            "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private final OrderService orderService;
    private final AppProperties appProperties;
    private final SpringTemplateEngine templateEngine;

    /** The bill of one order, ready to be shown on screen or rendered to PDF. */
    public BillView getBill(Long orderId) {
        return toBill(orderService.getView(orderId));
    }

    /** The same bill as a PDF document. */
    public byte[] renderPdf(BillView bill) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("bill", bill);
        String html = templateEngine.process("bills/bill", context);

        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html, resourceBaseUrl());
        renderer.layout();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        renderer.createPDF(out);
        return out.toByteArray();
    }

    BillView toBill(OrderView order) {
        AppProperties.Bill config = appProperties.getBill();
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();

        return BillView.builder()
                .orderId(order.getId())
                .billNumber(billNumber(order.getId(), config.getNumberPrefix()))
                .billDate(order.getOrderDate() == null ? "" : order.getOrderDate().format(DATE_FORMAT))
                .trackingId(trimToNull(order.getTrackingId()))
                .sellerName(sellerName(config))
                .sellerAddressLines(new ArrayList<>(
                        config.getAddressLines() == null ? List.of() : config.getAddressLines()))
                .sellerMobile(trimToNull(config.getMobile()))
                .sellerEmail(trimToNull(config.getEmail()))
                .sellerGstin(trimToNull(config.getGstin()))
                .buyerName(order.getCustomerName())
                .buyerAddressLines(splitLines(order.getCustomerAddress()))
                .buyerMobile(trimToNull(order.getCustomerMobileNumber()))
                .items(new ArrayList<>(order.getItems()))
                .totalQuantity(order.getTotalQuantity())
                .totalAmount(total)
                .totalInWords(amountInWords(total))
                .notes(trimToNull(order.getNotesSummary()))
                .footerNote(trimToNull(config.getFooterNote()))
                .build();
    }

    private String sellerName(AppProperties.Bill config) {
        String name = trimToNull(config.getSellerName());
        return name == null ? appProperties.getShopName() : name;
    }

    /** "AE-000017" - short enough to quote on WhatsApp, long enough to stay sortable. */
    private String billNumber(Long orderId, String prefix) {
        String safePrefix = prefix == null ? "" : prefix;
        return orderId == null ? safePrefix : safePrefix + String.format("%06d", orderId);
    }

    /** The address is stored as typed, so it is printed with the same line breaks. */
    private List<String> splitLines(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(text.split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList());
    }

    /**
     * Lets Flying Saucer find {@code images/logo.png} next to the stylesheet, whether the app runs
     * from the exploded classes or from the packaged jar.
     */
    private String resourceBaseUrl() {
        URL staticRoot = getClass().getResource("/static/");
        if (staticRoot == null) {
            log.debug("No /static/ folder on the classpath - the bill is rendered without the logo");
            return null;
        }
        return staticRoot.toExternalForm();
    }

    /** "Rupees One Thousand Two Hundred and Fifty Only", in the Indian numbering system. */
    String amountInWords(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = rounded.longValue();
        int paise = rounded.subtract(BigDecimal.valueOf(rupees)).movePointRight(2).intValue();

        StringBuilder words = new StringBuilder("Rupees ").append(indianWords(rupees));
        if (paise > 0) {
            words.append(" and ").append(indianWords(paise)).append(" Paise");
        }
        return words.append(" Only").toString();
    }

    private String indianWords(long number) {
        if (number == 0) {
            return "Zero";
        }
        StringBuilder words = new StringBuilder();
        appendScale(words, number / 10_000_000, "Crore");
        appendScale(words, (number / 100_000) % 100, "Lakh");
        appendScale(words, (number / 1_000) % 100, "Thousand");
        appendScale(words, (number / 100) % 10, "Hundred");

        long remainder = number % 100;
        if (remainder > 0) {
            if (words.length() > 0) {
                words.append(" and ");
            }
            words.append(belowHundred(remainder));
        }
        return words.toString().trim();
    }

    private void appendScale(StringBuilder words, long value, String scale) {
        if (value == 0) {
            return;
        }
        if (words.length() > 0) {
            words.append(' ');
        }
        words.append(belowHundred(value)).append(' ').append(scale);
    }

    private String belowHundred(long value) {
        if (value < 20) {
            return UNITS[(int) value];
        }
        String tens = TENS[(int) (value / 10)];
        long unit = value % 10;
        return unit == 0 ? tens : tens + " " + UNITS[(int) unit];
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
