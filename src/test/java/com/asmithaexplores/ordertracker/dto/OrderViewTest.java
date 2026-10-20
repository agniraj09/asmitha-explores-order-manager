package com.asmithaexplores.ordertracker.dto;

import com.asmithaexplores.ordertracker.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderViewTest {

    private OrderView order;

    @BeforeEach
    void setUp() {
        order = OrderView.builder()
                .id(42L)
                .customerName("Meera")
                .customerMobileNumber("9876543210")
                .orderStatus(OrderStatus.SHIPPED)
                .trackingId("FE7781234567")
                .totalAmount(new BigDecimal("698.00"))
                .items(List.of(OrderItemView.builder()
                        .productName("Chess Flash Cards")
                        .quantity(2)
                        .build()))
                .build();
    }

    @Test
    @DisplayName("The tracking message carries the courier link it is given")
    void trackingMessageIncludesTheGivenTrackingUrl() {
        String message = order.trackingMessage("https://franchexpress.com/courier-tracking/");

        assertThat(message)
                .contains("Hi Meera")
                .contains("order #42 has been shipped")
                .contains("Tracking ID: FE7781234567")
                .contains("Track here: https://franchexpress.com/courier-tracking/")
                .contains("Chess Flash Cards x2")
                .contains("698.00");
    }

    @Test
    @DisplayName("A blank courier link leaves the tracking line out altogether")
    void trackingMessageDropsTheLineWhenNoUrlIsGiven() {
        assertThat(order.trackingMessage("   ")).doesNotContain("Track here:");
        assertThat(order.getTrackingMessage()).doesNotContain("Track here:");
    }

    @Test
    @DisplayName("The template keeps the placeholder so the browser can swap the link")
    void trackingMessageTemplateKeepsThePlaceholder() {
        String template = order.getTrackingMessageTemplate();

        assertThat(template).contains("Track here: " + OrderView.TRACKING_URL_PLACEHOLDER);
        assertThat(template.replace(OrderView.TRACKING_URL_PLACEHOLDER, "https://example.com/t/"))
                .isEqualTo(order.trackingMessage("https://example.com/t/"));
    }

    @Test
    @DisplayName("Dropping the placeholder line is what the browser does for a blank link")
    void droppingThePlaceholderLineMatchesTheBlankUrlMessage() {
        String withoutLink = order.getTrackingMessageTemplate().lines()
                .filter(line -> !line.contains(OrderView.TRACKING_URL_PLACEHOLDER))
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        assertThat(withoutLink).isEqualTo(order.getTrackingMessage());
    }

    @Test
    @DisplayName("The WhatsApp number is the customer mobile with the country code")
    void whatsAppNumberIsPrefixedWithTheCountryCode() {
        assertThat(order.getWhatsAppNumber()).isEqualTo("919876543210");
        assertThat(OrderView.builder().build().getWhatsAppNumber()).isEmpty();
    }
}
