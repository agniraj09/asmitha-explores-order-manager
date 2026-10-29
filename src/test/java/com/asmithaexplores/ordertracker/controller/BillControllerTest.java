package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.OrderCreationResult;
import com.asmithaexplores.ordertracker.dto.OrderForm;
import com.asmithaexplores.ordertracker.dto.OrderItemForm;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import com.asmithaexplores.ordertracker.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The bill endpoint hands back a real PDF for an order that exists. */
@SpringBootTest
@AutoConfigureMockMvc
class BillControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;

    /** Built once: the three checks only read the order, so they can share it. */
    private static Long orderId;

    @BeforeEach
    void setUp() {
        if (orderId != null) {
            return;
        }
        Customer customer = new Customer();
        customer.setName("Bill Test Customer");
        customer.setMobileNumber("9000000001");
        customer.setAddress("5 Beach Road\nChennai 600041");
        customer = customerRepository.save(customer);

        Product product = new Product();
        product.setName("Bill Test Flash Cards");
        product.setCategory("Flash Cards");
        product.setPrice(new BigDecimal("150.00"));
        product.setQuantityInHand(10);
        product = productRepository.save(product);

        OrderForm form = new OrderForm();
        form.setCustomerId(customer.getId());
        form.setOrderDate(LocalDate.now());
        OrderItemForm item = new OrderItemForm();
        item.setProductId(product.getId());
        item.setQuantity(2);
        form.setItems(List.of(item));

        OrderCreationResult result = orderService.create(form);
        orderId = result.getOrderId();
    }

    @Test
    @DisplayName("The bill downloads as a PDF attachment named after the bill number")
    void downloadsBillAsPdf() throws Exception {
        MvcResult result = mockMvc.perform(get("/orders/{id}/bill", orderId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("attachment")))
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("The preview link asks the browser to show the bill instead of saving it")
    void previewsBillInline() throws Exception {
        mockMvc.perform(get("/orders/{id}/bill", orderId).param("inline", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.startsWith("inline")));
    }

    @Test
    @DisplayName("An unknown order has no bill")
    void unknownOrderIsNotFound() throws Exception {
        mockMvc.perform(get("/orders/{id}/bill", 999_999L))
                .andExpect(status().isNotFound());
    }
}
