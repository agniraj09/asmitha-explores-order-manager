package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.RestockResult;
import com.asmithaexplores.ordertracker.dto.RestockView;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.repository.OrderItemRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ProductService productService;

    private Product flashCards;

    @BeforeEach
    void setUp() {
        flashCards = Product.builder()
                .id(10L)
                .name("Tamil 247 Letters Flash Cards")
                .price(new BigDecimal("349.00"))
                .quantityInHand(0)
                .quantityToBeOrdered(6)
                .build();
    }

    @Test
    @DisplayName("To print is the booked shortfall, not open orders minus stock in hand")
    void requiredQuantityIgnoresUnitsAlreadyDeductedFromStock() {
        flashCards.setQuantityInHand(0);
        when(productRepository.findNeedingRestock()).thenReturn(List.of(flashCards));
        when(orderItemRepository.sumUnitsInOpenOrders())
                .thenReturn(List.<Object[]>of(new Object[]{10L, 8}));

        List<RestockView> restock = productService.findNeedingRestock();

        assertThat(restock).hasSize(1);
        assertThat(restock.get(0).getUnitsInOpenOrders()).isEqualTo(8);
        assertThat(restock.get(0).getRequiredQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("Ordered adds the printed units to stock and clears the backlog")
    void markOrderedAddsPrintedUnitsToStock() {
        flashCards.setQuantityInHand(2);
        when(productRepository.findById(10L)).thenReturn(Optional.of(flashCards));
        when(productRepository.save(flashCards)).thenReturn(flashCards);

        RestockResult result = productService.markOrdered(10L);

        assertThat(result.quantityAdded()).isEqualTo(6);
        assertThat(flashCards.getQuantityToBeOrdered()).isZero();
        assertThat(flashCards.getQuantityInHand()).isEqualTo(8);
    }

    @Test
    @DisplayName("Ordered is rejected when nothing is booked to be printed")
    void markOrderedRejectsWhenNothingPending() {
        flashCards.setQuantityToBeOrdered(0);
        when(productRepository.findById(10L)).thenReturn(Optional.of(flashCards));

        assertThatThrownBy(() -> productService.markOrdered(10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Nothing to order");
    }
}
