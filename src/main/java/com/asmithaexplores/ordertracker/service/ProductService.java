package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.config.AppProperties;
import com.asmithaexplores.ordertracker.dto.ProductForm;
import com.asmithaexplores.ordertracker.dto.ProductView;
import com.asmithaexplores.ordertracker.dto.RestockResult;
import com.asmithaexplores.ordertracker.dto.RestockView;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.exception.ResourceNotFoundException;
import com.asmithaexplores.ordertracker.repository.OrderItemRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    /** Same categories the storefront (asmitha-explores-bookstore) uses. */
    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Flash Cards", "Banners", "Learning Books", "Story Books", "Other");

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public List<ProductView> findAll() {
        return productRepository.findAllByOrderByCategoryAscNameAsc().stream().map(this::toView).toList();
    }

    /** Products grouped by category - used to build &lt;optgroup&gt;s on the order form. */
    public Map<String, List<ProductView>> findAllGroupedByCategory() {
        return findAll().stream().collect(Collectors.groupingBy(
                p -> p.getCategory() == null || p.getCategory().isBlank() ? "Other" : p.getCategory(),
                LinkedHashMap::new,
                Collectors.toList()));
    }

    /** Known categories: whatever is already in the catalogue plus the storefront defaults. */
    public List<String> findCategories() {
        Set<String> categories = new LinkedHashSet<>(DEFAULT_CATEGORIES);
        categories.addAll(productRepository.findDistinctCategories());
        return new ArrayList<>(categories);
    }

    public List<Product> findAllProducts() {
        return productRepository.findAllByOrderByNameAsc();
    }

    /** Products that have run out completely - the only ones highlighted in the UI. */
    public List<ProductView> findOutOfStock() {
        return productRepository.findOutOfStock().stream()
                .map(this::toView)
                .toList();
    }

    /**
     * Dashboard list: products that still owe units to open orders, i.e. a shortfall was booked
     * because stock could not cover an order. See
     * {@link com.asmithaexplores.ordertracker.repository.ProductRepository#findNeedingRestock()}.
     */
    public List<RestockView> findNeedingRestock() {
        Map<Long, Integer> openUnits = new HashMap<>();
        for (Object[] row : orderItemRepository.sumUnitsInOpenOrders()) {
            openUnits.put((Long) row[0], ((Number) row[1]).intValue());
        }

        return productRepository.findNeedingRestock().stream()
                .map(product -> RestockView.builder()
                        .id(product.getId())
                        .name(product.getName())
                        .category(product.getCategory())
                        .quantityInHand(product.getQuantityInHand())
                        .quantityToBeOrdered(product.getQuantityToBeOrdered())
                        .unitsInOpenOrders(openUnits.getOrDefault(product.getId(), 0))
                        .outOfStock(product.getQuantityInHand() <= 0)
                        .build())
                .toList();
    }

    public Product getEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    public ProductForm getForm(Long id) {
        Product product = getEntity(id);
        return ProductForm.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .quantityInHand(product.getQuantityInHand())
                .quantityToBeOrdered(product.getQuantityToBeOrdered())
                .build();
    }

    @Transactional
    public Product save(ProductForm form) {
        Product product = form.getId() == null ? new Product() : getEntity(form.getId());
        product.setName(form.getName().trim());
        product.setCategory(form.getCategory() == null ? null : form.getCategory().trim());
        product.setPrice(form.getPrice());
        product.setQuantityInHand(form.getQuantityInHand());
        product.setQuantityToBeOrdered(form.getQuantityToBeOrdered());
        return productRepository.save(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = getEntity(id);
        if (orderItemRepository.existsByProductId(id)) {
            throw new BusinessRuleException(
                    "Cannot delete " + product.getName() + " because it is used in existing orders.");
        }
        productRepository.delete(product);
    }

    /**
     * "Ordered" action on the dashboard: the owner has printed / ordered the units that were
     * booked as missing, so those units are now physically in hand and the backlog is cleared.
     * They are deducted again when the order they belong to reaches
     * {@link com.asmithaexplores.ordertracker.entity.OrderStatus#DELIVERED}.
     *
     * @return the product name and how many units were added
     */
    @Transactional
    public RestockResult markOrdered(Long productId) {
        Product product = getEntity(productId);

        int required = Math.max(0, product.getQuantityToBeOrdered());

        if (required <= 0) {
            throw new BusinessRuleException(
                    "Nothing to order for " + product.getName() + " - stock already covers the open orders.");
        }

        product.setQuantityInHand(product.getQuantityInHand() + required);
        product.setQuantityToBeOrdered(0);
        productRepository.save(product);

        return new RestockResult(product.getName(), required,
                product.getQuantityInHand(), product.getQuantityToBeOrdered());
    }

    public ProductView toView(Product product) {
        return ProductView.builder()
                .id(product.getId())
                .name(product.getName())
                .category(product.getCategory())
                .price(product.getPrice())
                .quantityInHand(product.getQuantityInHand())
                .quantityToBeOrdered(product.getQuantityToBeOrdered())
                .outOfStock(product.getQuantityInHand() <= 0)
                .build();
    }
}

