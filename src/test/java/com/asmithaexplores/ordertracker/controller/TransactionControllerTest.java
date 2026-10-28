package com.asmithaexplores.ordertracker.controller;

import com.asmithaexplores.ordertracker.dto.OrderForm;
import com.asmithaexplores.ordertracker.dto.OrderItemForm;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.entity.Transaction;
import com.asmithaexplores.ordertracker.entity.TransactionCategory;
import com.asmithaexplores.ordertracker.entity.TransactionType;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import com.asmithaexplores.ordertracker.repository.TransactionRepository;
import com.asmithaexplores.ordertracker.repository.TransactionTagRepository;
import com.asmithaexplores.ordertracker.service.OrderService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End to end checks for the money book: the screens render, a manual expense can be entered with
 * tags, and placing an order books the matching income line.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private OrderService orderService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionTagRepository tagRepository;

    private Long bannerTagId;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        // "Banners" is one of the tags created on an empty database by the seeder.
        bannerTagId = tagRepository.findByNameIgnoreCase("Banners").orElseThrow().getId();
    }

    @Test
    @DisplayName("The finance list renders with its summary")
    void listRenders() throws Exception {
        mockMvc.perform(get("/transactions"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/list"))
                .andExpect(model().attributeExists("transactions", "summary", "tags"))
                .andExpect(content().string(Matchers.containsString("Finance")));
    }

    @Test
    @DisplayName("The expense form and the tag screen render")
    void formsRender() throws Exception {
        mockMvc.perform(get("/transactions/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/form"));

        mockMvc.perform(get("/transactions/tags"))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/tags"))
                .andExpect(model().attributeExists("tags", "tagForm"));
    }

    @Test
    @DisplayName("The income entry point opens the form ready for an untracked order sale")
    void incomeFormDefaultsToOrderSale() throws Exception {
        mockMvc.perform(get("/transactions/new").param("type", TransactionType.INCOME.name()))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/form"))
                .andExpect(model().attribute("formType", TransactionType.INCOME));
    }

    @Test
    @DisplayName("Income from a past order that is not in the system is saved without an order link")
    void savesManualIncomeForUntrackedOrder() throws Exception {
        mockMvc.perform(post("/transactions/save")
                        .param("category", TransactionCategory.ORDER_SALE.name())
                        .param("amount", "3200.00")
                        .param("transactionDate", LocalDate.now().minusMonths(2).toString())
                        .param("description", "Offline sale - 20 flash card sets")
                        .param("tagIds", String.valueOf(bannerTagId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions"));

        List<Transaction> saved = transactionRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getType()).isEqualTo(TransactionType.INCOME);
        assertThat(saved.get(0).getOrder()).isNull();
        assertThat(saved.get(0).isSystemGenerated()).isFalse();
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo("3200.00");
    }

    @Test
    @DisplayName("A manual expense is saved with the tags that were ticked")
    void savesManualExpenseWithTags() throws Exception {
        mockMvc.perform(post("/transactions/save")
                        .param("category", TransactionCategory.PRINTING.name())
                        .param("amount", "1250.50")
                        .param("transactionDate", LocalDate.now().toString())
                        .param("description", "Vinyl banner printing")
                        .param("tagIds", String.valueOf(bannerTagId)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/transactions"));

        List<Transaction> saved = transactionRepository.findAll();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getType()).isEqualTo(TransactionType.EXPENSE);
        assertThat(saved.get(0).getAmount()).isEqualByComparingTo("1250.50");
        assertThat(saved.get(0).getTags()).extracting("name").containsExactly("Banners");
    }

    @Test
    @DisplayName("An invalid amount keeps the user on the form")
    void rejectsInvalidAmount() throws Exception {
        mockMvc.perform(post("/transactions/save")
                        .param("category", TransactionCategory.PACKING.name())
                        .param("amount", "0")
                        .param("transactionDate", LocalDate.now().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("transactions/form"))
                .andExpect(model().attributeHasFieldErrors("transactionForm", "amount"));

        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    @DisplayName("Placing an order books the income and tags it with the product categories")
    void orderCreatesIncomeLine() {
        Customer customer = customerRepository.save(Customer.builder()
                .name("Meera Krishnan")
                .address("Chennai")
                .mobileNumber("9840099999")
                .build());
        Product banner = productRepository.save(Product.builder()
                .name("Numbers - Banner 1 to 50")
                .category("Banners")
                .price(new BigDecimal("650.00"))
                .quantityInHand(5)
                .quantityToBeOrdered(0)
                .build());

        Long orderId = orderService.create(OrderForm.builder()
                .customerId(customer.getId())
                .orderDate(LocalDate.now())
                .items(List.of(new OrderItemForm(banner.getId(), 2)))
                .build()).getOrderId();

        Optional<Transaction> income = transactionRepository.findByOrderId(orderId);
        assertThat(income).isPresent();
        assertThat(income.get().getType()).isEqualTo(TransactionType.INCOME);
        assertThat(income.get().getCategory()).isEqualTo(TransactionCategory.ORDER_SALE);
        assertThat(income.get().getAmount()).isEqualByComparingTo("1300.00");
        assertThat(income.get().isSystemGenerated()).isTrue();
        assertThat(income.get().getTags()).extracting("name").containsExactly("Banners");

        // Deleting the order must take its income with it, otherwise the books show a phantom sale.
        orderService.delete(orderId);
        assertThat(transactionRepository.findByOrderId(orderId)).isEmpty();
    }

    @Test
    @DisplayName("Editing an order income by hand stops it from following the order")
    void editedIncomeIsNoLongerSynced() throws Exception {
        Customer customer = customerRepository.save(Customer.builder()
                .name("Arjun Reddy")
                .address("Hyderabad")
                .mobileNumber("9701188888")
                .build());
        Product cards = productRepository.save(Product.builder()
                .name("Phonics Flash Cards")
                .category("Flash Cards")
                .price(new BigDecimal("300.00"))
                .quantityInHand(20)
                .quantityToBeOrdered(0)
                .build());

        Long orderId = orderService.create(OrderForm.builder()
                .customerId(customer.getId())
                .orderDate(LocalDate.now())
                .items(List.of(new OrderItemForm(cards.getId(), 1)))
                .build()).getOrderId();

        Long incomeId = transactionRepository.findByOrderId(orderId).orElseThrow().getId();

        mockMvc.perform(post("/transactions/save")
                        .param("id", String.valueOf(incomeId))
                        .param("category", TransactionCategory.ORDER_SALE.name())
                        .param("amount", "250.00")
                        .param("transactionDate", LocalDate.now().toString())
                        .param("description", "Discount agreed on WhatsApp"))
                .andExpect(status().is3xxRedirection());

        // Growing the order would normally re-book the income - the corrected figure must survive.
        orderService.update(OrderForm.builder()
                .id(orderId)
                .customerId(customer.getId())
                .orderDate(LocalDate.now())
                .items(List.of(new OrderItemForm(cards.getId(), 3)))
                .build());

        Transaction income = transactionRepository.findByOrderId(orderId).orElseThrow();
        assertThat(income.isManuallyAdjusted()).isTrue();
        assertThat(income.getAmount()).isEqualByComparingTo("250.00");
    }
}
