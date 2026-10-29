package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.asmithaexplores.ordertracker.dto.BillView;
import com.asmithaexplores.ordertracker.dto.OrderItemView;
import com.asmithaexplores.ordertracker.dto.OrderView;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillServiceTest {

    @Mock
    private OrderService orderService;

    private BillService billService;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        billService = new BillService(orderService, new AppProperties(), engine);
    }

    private OrderView order() {
        return OrderView.builder()
                .id(17L)
                .customerName("Meera")
                .customerMobileNumber("9876543210")
                .customerAddress("12 Gandhi Street\nAdyar\nChennai 600020")
                .orderDate(LocalDate.of(2026, 1, 9))
                .orderStatus(OrderStatus.PACKED)
                .trackingId("FE123456")
                .specialNotes("Gift wrap please")
                .items(List.of(OrderItemView.builder()
                        .productName("Flash cards")
                        .quantity(2)
                        .unitPrice(new BigDecimal("250.00"))
                        .lineTotal(new BigDecimal("500.00"))
                        .build()))
                .totalQuantity(2)
                .totalAmount(new BigDecimal("500.00"))
                .build();
    }

    @Test
    @DisplayName("The bill carries the seller block, the customer block, the items and the total")
    void buildsBillFromOrder() {
        when(orderService.getView(17L)).thenReturn(order());

        BillView bill = billService.getBill(17L);

        assertThat(bill.getBillNumber()).isEqualTo("AE-000017");
        assertThat(bill.getBillDate()).isEqualTo("09 Jan 2026");
        assertThat(bill.getFileName()).isEqualTo("Bill_AE-000017.pdf");
        assertThat(bill.getSellerName()).isEqualTo("Asmitha Explores");
        assertThat(bill.getSellerAddressLines()).containsExactly("OMR", "Chennai - 600097");
        assertThat(bill.getSellerMobile()).isEqualTo("9445077125");
        assertThat(bill.getBuyerName()).isEqualTo("Meera");
        assertThat(bill.getBuyerAddressLines())
                .containsExactly("12 Gandhi Street", "Adyar", "Chennai 600020");
        assertThat(bill.getItems()).hasSize(1);
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("500.00");
        assertThat(bill.getTotalInWords()).isEqualTo("Rupees Five Hundred Only");
        assertThat(bill.getNotes()).contains("Gift wrap please");
    }

    @Test
    @DisplayName("The bill renders into a real PDF document")
    void rendersPdf() {
        when(orderService.getView(17L)).thenReturn(order());

        byte[] pdf = billService.renderPdf(billService.getBill(17L));

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Totals are spelled out in the Indian numbering system")
    void spellsAmountsOut() {
        assertThat(billService.amountInWords(BigDecimal.ZERO)).isEqualTo("Rupees Zero Only");
        assertThat(billService.amountInWords(new BigDecimal("1250")))
                .isEqualTo("Rupees One Thousand Two Hundred and Fifty Only");
        assertThat(billService.amountInWords(new BigDecimal("125000.50")))
                .isEqualTo("Rupees One Lakh Twenty Five Thousand and Fifty Paise Only");
        assertThat(billService.amountInWords(new BigDecimal("10000000")))
                .isEqualTo("Rupees One Crore Only");
    }
}
