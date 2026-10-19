package com.asmithaexplores.ordertracker.config;

import com.asmithaexplores.ordertracker.dto.OrderForm;
import com.asmithaexplores.ordertracker.dto.OrderItemForm;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import com.asmithaexplores.ordertracker.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Inserts the starting data the first time the application runs on an empty database.
 * The catalogue mirrors the Asmitha Explores storefront
 * (https://asmitha-explores-bookstore.onrender.com): names, categories and prices.
 * Stock numbers are placeholders - correct them from the Inventory screen.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String FLASH_CARDS = "Flash Cards";
    private static final String BANNERS = "Banners";
    private static final String LEARNING_BOOKS = "Learning Books";
    private static final String STORY_BOOKS = "Story Books";

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final AppProperties appProperties;

    @Override
    @Transactional
    public void run(String... args) {
        if (!appProperties.isSeedData()) {
            return;
        }
        if (customerRepository.count() > 0 || productRepository.count() > 0 || orderRepository.count() > 0) {
            log.info("Database already contains data - skipping seed.");
            return;
        }

        log.info("Seeding {} catalogue and demo orders ...", appProperties.getShopName());

        Map<String, Product> catalogue = seedCatalogue();
        seedCustomersAndOrders(catalogue);

        log.info("Seed complete: {} customers, {} products, {} orders.",
                customerRepository.count(), productRepository.count(), orderRepository.count());
    }

    /* -------------------------------------------------- real storefront catalogue */

    private Map<String, Product> seedCatalogue() {
        List<Product> products = List.of(
                // ---------------- Flash Cards ----------------
                product("Tamil 247 Letters Flash Cards", FLASH_CARDS, "1350.00", 6, 0),
                product("Alphabet Picture Flash Cards", FLASH_CARDS, "600.00", 12, 0),
                product("Tamil Uyir Mei Eluthukkal Flash Cards", FLASH_CARDS, "360.00", 8, 0),
                product("Consonant Blending & Digraph Flash Cards", FLASH_CARDS, "360.00", 7, 0),
                product("Wild & Domestic Animals Flash Cards", FLASH_CARDS, "360.00", 9, 0),
                product("Flags Flash Cards A to Z", FLASH_CARDS, "320.00", 5, 0),
                product("Phonics Flash Cards", FLASH_CARDS, "300.00", 10, 0),
                product("Parts of Body Flash Cards", FLASH_CARDS, "300.00", 6, 0),
                product("Tamil Mei Eluthukkal Flash Cards", FLASH_CARDS, "280.00", 4, 0),
                product("Birds Flash Cards", FLASH_CARDS, "250.00", 8, 0),
                product("Tamil Uyir Eluthukkal Flash Cards", FLASH_CARDS, "220.00", 3, 0),
                product("Flowers Flash Cards", FLASH_CARDS, "180.00", 11, 0),

                // ---------------- Banners ----------------
                product("Numbers & Tamil Names - Banner 1 to 100", BANNERS, "850.00", 2, 0),
                product("Maths Multiplication Table 1 to 10 - Banner", BANNERS, "750.00", 3, 0),
                product("Jolly Phonics - Banner", BANNERS, "680.00", 2, 0),
                product("Phonics Flash Cards - Banner", BANNERS, "650.00", 1, 0),
                product("Jolly Phonics - Sound Group Banner", BANNERS, "650.00", 2, 0),
                product("Numbers - Banner 1 to 50", BANNERS, "650.00", 4, 0),
                product("Tamil Uyir Mei Eluthukkal - Banner", BANNERS, "650.00", 0, 3),
                product("Tamil Uyir Mei Eluthukkal Table - Banner", BANNERS, "650.00", 1, 0),
                product("Tamil Uyir Eluthukkal - Banner", BANNERS, "600.00", 2, 0),
                product("Tamil Mei Eluthukkal - Banner", BANNERS, "600.00", 2, 0),

                // ---------------- Learning Books ----------------
                product("Tamil Phonics Book", LEARNING_BOOKS, "599.00", 5, 0),
                product("Having Fun With Phonics", LEARNING_BOOKS, "199.00", 14, 0),
                product("Tamil Letters Book",
                        LEARNING_BOOKS, "189.00", 10, 0),
                product("Alphabets Writing Practice", LEARNING_BOOKS, "189.00", 15, 0),

                // ---------------- Story Books ----------------
                product("Bed Time Story Book - Penguin Theme", STORY_BOOKS, "100.00", 20, 0),
                product("Bed Time Story Book - Giraffe Theme", STORY_BOOKS, "100.00", 18, 0),
                product("Bed Time Story Book - Rabbit Theme", STORY_BOOKS, "100.00", 4, 0));

        return productRepository.saveAll(products).stream()
                .collect(Collectors.toMap(Product::getName, Function.identity(), (a, b) -> a));
    }

    private Product product(String name, String category, String price, int inHand, int toBeOrdered) {
        return Product.builder()
                .name(name)
                .category(category)
                .price(new BigDecimal(price))
                .quantityInHand(inHand)
                .quantityToBeOrdered(toBeOrdered)
                .build();
    }

    /* ------------------------------------------------------ demo customers/orders */

    private void seedCustomersAndOrders(Map<String, Product> catalogue) {
        Customer meera = customerRepository.save(Customer.builder()
                .name("Meera Krishnan")
                .address("12, Lake View Apartments\nAnna Nagar\nChennai 600040")
                .mobileNumber("9840012345")
                .build());
        Customer arjun = customerRepository.save(Customer.builder()
                .name("Arjun Reddy")
                .address("Flat 402, Sai Residency\nGachibowli\nHyderabad 500032")
                .mobileNumber("9701123456")
                .build());
        Customer divya = customerRepository.save(Customer.builder()
                .name("Divya Sharma")
                .address("House No 8, Sector 21\nDwarka\nNew Delhi 110075")
                .mobileNumber("9811234567")
                .build());

        Long order1 = orderService.create(OrderForm.builder()
                .customerId(meera.getId())
                .orderDate(LocalDate.now().minusDays(9))
                .customizationNotes("Print \"Aarav\" on the flash card box lid.")
                .items(List.of(
                        item(catalogue, "Alphabet Picture Flash Cards", 2),
                        item(catalogue, "Bed Time Story Book - Rabbit Theme", 1)))
                .build()).getOrderId();

        Long order2 = orderService.create(OrderForm.builder()
                .customerId(arjun.getId())
                .orderDate(LocalDate.now().minusDays(4))
                .customizationNotes("Banner for Aadhya's classroom - pastel theme, laminated.")
                .items(List.of(
                        item(catalogue, "Tamil Uyir Mei Eluthukkal - Banner", 1),
                        item(catalogue, "Numbers - Banner 1 to 50", 1)))
                .build()).getOrderId();

        orderService.create(OrderForm.builder()
                .customerId(divya.getId())
                .orderDate(LocalDate.now().minusDays(1))
                .customizationNotes("Gift wrap, no invoice inside.")
                .items(List.of(
                        item(catalogue, "Phonics Flash Cards", 2),
                        item(catalogue, "Having Fun With Phonics", 1)))
                .build());

        // The first order went all the way to delivered, the second one is being printed.
        orderService.updateStatus(order1, OrderStatus.PRINTING_IN_PROGRESS, null);
        orderService.updateStatus(order1, OrderStatus.PACKED, null);
        orderService.updateStatus(order1, OrderStatus.SHIPPED, "DTDC7781234567");
        orderService.updateStatus(order1, OrderStatus.DELIVERED, null);
        orderService.updateStatus(order2, OrderStatus.PRINTING_IN_PROGRESS, null);
    }

    private OrderItemForm item(Map<String, Product> catalogue, String productName, int quantity) {
        Product product = catalogue.get(productName);
        if (product == null) {
            throw new IllegalStateException("Seed product not found: " + productName);
        }
        return new OrderItemForm(product.getId(), quantity);
    }
}

