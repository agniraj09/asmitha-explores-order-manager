package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.AddressLabelView;
import com.asmithaexplores.ordertracker.dto.OrderCreationResult;
import com.asmithaexplores.ordertracker.dto.OrderEditForm;
import com.asmithaexplores.ordertracker.dto.OrderFilter;
import com.asmithaexplores.ordertracker.dto.OrderForm;
import com.asmithaexplores.ordertracker.dto.OrderItemForm;
import com.asmithaexplores.ordertracker.dto.OrderItemView;
import com.asmithaexplores.ordertracker.dto.OrderPeriodSummary;
import com.asmithaexplores.ordertracker.dto.OrderView;
import com.asmithaexplores.ordertracker.entity.Customer;
import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderItem;
import com.asmithaexplores.ordertracker.entity.OrderStatus;
import com.asmithaexplores.ordertracker.entity.Product;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.exception.ResourceNotFoundException;
import com.asmithaexplores.ordertracker.repository.CustomerRepository;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.repository.ProductRepository;
import com.asmithaexplores.ordertracker.tracking.TrackingSnapshot;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final TransactionService transactionService;

    /* ------------------------------------------------------------------ reads */

    public List<OrderView> search(OrderFilter filter) {
        OrderFilter f = filter == null ? new OrderFilter() : filter;
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.getStatus() != null) {
                predicates.add(cb.equal(root.get("orderStatus"), f.getStatus()));
            }
            if (f.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), f.getCustomerId()));
            }
            if (f.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("orderDate"), f.getFromDate()));
            }
            if (f.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("orderDate"), f.getToDate()));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("orderDate")), cb.desc(root.get("id")));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
        return orderRepository.findAll(spec).stream().map(OrderService::toView).toList();
    }

    public List<OrderView> findByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDescIdDesc(customerId).stream()
                .map(OrderService::toView)
                .toList();
    }

    public List<OrderView> findPendingOrders() {
        return orderRepository.findByOrderStatusNotOrderByOrderDateAscIdAsc(OrderStatus.DELIVERED).stream()
                .map(OrderService::toView)
                .toList();
    }

    /**
     * Shipping labels for every order that is still open (anything but delivered), oldest first.
     * Orders that have not been packed yet - {@link OrderStatus#NEW} and
     * {@link OrderStatus#PRINTING_IN_PROGRESS} - are the ones the labels are usually printed for,
     * so only those are ticked on load; the rest can be added from the same page.
     */
    public List<AddressLabelView> findLabelCandidates() {
        return orderRepository.findByOrderStatusNotOrderByOrderDateAscIdAsc(OrderStatus.DELIVERED).stream()
                .map(OrderService::toLabel)
                .toList();
    }

    private static AddressLabelView toLabel(Order order) {
        Customer customer = order.getCustomer();
        OrderStatus status = order.getOrderStatus();
        return AddressLabelView.builder()
                .orderId(order.getId())
                .customerName(customer == null ? "-" : customer.getName())
                .statusLabel(status == null ? "-" : status.getLabel())
                .statusBadgeClass(status == null ? "secondary" : status.getBadgeClass())
                .orderDateLabel(order.getOrderDate() == null ? "" : DATE_FORMAT.format(order.getOrderDate()))
                .addressLines(addressLines(customer))
                .phones(phones(customer))
                .selectedByDefault(status == OrderStatus.NEW || status == OrderStatus.PRINTING_IN_PROGRESS)
                .build();
    }

    private static List<String> addressLines(Customer customer) {
        if (customer == null || customer.getAddress() == null) {
            return new ArrayList<>();
        }
        return Arrays.stream(customer.getAddress().split("\\R"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** Primary number first; the alternate one only when it is actually a different number. */
    private static List<String> phones(Customer customer) {
        List<String> phones = new ArrayList<>();
        if (customer == null) {
            return phones;
        }
        String primary = trimToNull(customer.getMobileNumber());
        String alternate = trimToNull(customer.getAlternateMobileNumber());
        if (primary != null) {
            phones.add(primary);
        }
        if (alternate != null && !alternate.equals(primary)) {
            phones.add(alternate);
        }
        return phones;
    }

    public Order getEntity(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", id));
    }

    public OrderView getView(Long id) {
        return toView(getEntity(id));
    }

    public Map<OrderStatus, Long> countPerStatus() {
        Map<OrderStatus, Long> counts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0L);
        }
        for (Object[] row : orderRepository.countGroupedByStatus()) {
            counts.put((OrderStatus) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    /** How many orders were placed between the two dates and what they were worth. */
    public OrderPeriodSummary summariseBetween(LocalDate from, LocalDate to) {
        List<Object[]> rows = orderRepository.summariseOrdersBetween(from, to);
        if (rows.isEmpty()) {
            return new OrderPeriodSummary(0L, BigDecimal.ZERO);
        }
        Object[] row = rows.get(0);
        long count = row[0] == null ? 0L : ((Number) row[0]).longValue();
        BigDecimal value = row[1] == null ? BigDecimal.ZERO : (BigDecimal) row[1];
        return new OrderPeriodSummary(count, value);
    }

    /* ----------------------------------------------------------------- writes */

    /**
     * Creates an order and deducts stock. When stock is insufficient the order is still
     * accepted: {@code quantityInHand} drops to 0 and the shortfall is added to
     * {@code quantityToBeOrdered} so the owner knows how much to print/re-order.
     */
    @Transactional
    public OrderCreationResult create(OrderForm form) {
        Customer customer = resolveCustomer(form);
        Map<Long, Integer> quantitiesByProduct = mergeItems(form.getItems());
        if (quantitiesByProduct.isEmpty()) {
            throw new BusinessRuleException("Add at least one product with a quantity of 1 or more.");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(form.getOrderDate() == null ? LocalDate.now() : form.getOrderDate());
        order.setOrderStatus(OrderStatus.NEW);
        order.setCustomizationNotes(trimToNull(form.getCustomizationNotes()));
        order.setSpecialNotes(trimToNull(form.getSpecialNotes()));

        List<String> warnings = addItemsAndDeductStock(order, quantitiesByProduct);

        Order saved = orderRepository.save(order);
        transactionService.recordOrderIncome(saved);
        return OrderCreationResult.builder().orderId(saved.getId()).warnings(warnings).build();
    }

    /**
     * Replaces the contents of an order that is still {@link OrderStatus#NEW}: customer, date,
     * notes and the product lines. The stock movement of the previous version is reversed first,
     * then the new lines are deducted, so inventory always matches the current order.
     */
    @Transactional
    public OrderCreationResult update(OrderForm form) {
        if (form.getId() == null) {
            throw new BusinessRuleException("Missing order id - cannot update the order.");
        }
        Order order = getEntity(form.getId());
        if (order.getOrderStatus() != OrderStatus.NEW) {
            throw new BusinessRuleException("Order #" + order.getId() + " is already "
                    + order.getOrderStatus().getLabel()
                    + ". Only orders that are still New can be edited.");
        }

        Customer customer = resolveCustomer(form);
        Map<Long, Integer> quantitiesByProduct = mergeItems(form.getItems());
        if (quantitiesByProduct.isEmpty()) {
            throw new BusinessRuleException("Add at least one product with a quantity of 1 or more.");
        }

        restoreStock(order);
        order.getOrderItems().clear();

        order.setCustomer(customer);
        order.setOrderDate(form.getOrderDate() == null ? LocalDate.now() : form.getOrderDate());
        order.setCustomizationNotes(trimToNull(form.getCustomizationNotes()));
        order.setSpecialNotes(trimToNull(form.getSpecialNotes()));

        List<String> warnings = addItemsAndDeductStock(order, quantitiesByProduct);

        Order saved = orderRepository.save(order);
        transactionService.syncOrderIncome(saved);
        return OrderCreationResult.builder().orderId(saved.getId()).warnings(warnings).build();
    }

    /** Pre-fills the order form for editing. Only possible while the order is NEW. */
    public OrderForm getEditForm(Long orderId) {
        Order order = getEntity(orderId);
        if (order.getOrderStatus() != OrderStatus.NEW) {
            throw new BusinessRuleException("Order #" + order.getId() + " is already "
                    + order.getOrderStatus().getLabel()
                    + ". Only orders that are still New can be edited.");
        }
        List<OrderItemForm> items = order.getOrderItems().stream()
                .map(i -> new OrderItemForm(i.getProduct().getId(), i.getQuantity()))
                .collect(Collectors.toCollection(ArrayList::new));

        return OrderForm.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .newCustomer(false)
                .items(items)
                .customizationNotes(order.getCustomizationNotes())
                .specialNotes(order.getSpecialNotes())
                .orderDate(order.getOrderDate())
                .build();
    }

    private Customer resolveCustomer(OrderForm form) {
        if (form.getCustomerId() == null) {
            throw new BusinessRuleException("Please select (or create) a customer for this order.");
        }
        return customerRepository.findById(form.getCustomerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", form.getCustomerId()));
    }

    /** Adds one order item per product and deducts stock, returning the shortfall warnings. */
    private List<String> addItemsAndDeductStock(Order order, Map<Long, Integer> quantitiesByProduct) {
        List<String> warnings = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantitiesByProduct.entrySet()) {
            Product product = productRepository.findById(entry.getKey())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", entry.getKey()));
            int quantity = entry.getValue();

            int shortfall = applyStockDeduction(product, quantity);

            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitPrice(product.getPrice());
            item.setShortfallQuantity(shortfall);
            order.addItem(item);

            if (shortfall > 0) {
                warnings.add(product.getName() + ": only " + (quantity - shortfall)
                        + " in hand, " + shortfall + " more added to \"to be ordered\".");
            }
            productRepository.save(product);
        }
        return warnings;
    }

    /** Exact reverse of {@link #applyStockDeduction}: puts the ordered quantities back. */
    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            int shortfall = Math.min(item.getShortfallQuantityOrZero(), item.getQuantity());
            product.setQuantityInHand(product.getQuantityInHand() + item.getQuantity() - shortfall);
            product.setQuantityToBeOrdered(Math.max(0, product.getQuantityToBeOrdered() - shortfall));
            productRepository.save(product);
        }
    }

    /** Deducts what is available and books the rest as to-be-ordered. Returns the shortfall. */
    private int applyStockDeduction(Product product, int quantity) {
        int available = product.getQuantityInHand();
        if (available >= quantity) {
            product.setQuantityInHand(available - quantity);
            return 0;
        }
        int shortfall = quantity - Math.max(available, 0);
        product.setQuantityInHand(0);
        product.setQuantityToBeOrdered(product.getQuantityToBeOrdered() + shortfall);
        return shortfall;
    }

    @Transactional
    public void updateDetails(OrderEditForm form) {
        Order order = getEntity(form.getId());
        order.setCustomizationNotes(trimToNull(form.getCustomizationNotes()));
        order.setSpecialNotes(trimToNull(form.getSpecialNotes()));
        String trackingId = trimToNull(form.getTrackingId());
        if (trackingId == null && order.getOrderStatus().ordinal() >= OrderStatus.SHIPPED.ordinal()) {
            throw new BusinessRuleException("A shipped order must keep its tracking id.");
        }
        order.setTrackingId(trackingId);
        orderRepository.save(order);
    }

    /**
     * Moves an order exactly one step forward in the status flow.
     * A tracking id is mandatory when moving to {@link OrderStatus#SHIPPED}.
     */
    @Transactional
    public OrderView updateStatus(Long orderId, OrderStatus targetStatus, String trackingId) {
        Order order = getEntity(orderId);
        OrderStatus current = order.getOrderStatus();

        if (targetStatus == null) {
            throw new BusinessRuleException("Please choose a status.");
        }
        if (targetStatus == current) {
            throw new BusinessRuleException("The order is already " + current.getLabel() + ".");
        }
        if (!current.canMoveTo(targetStatus)) {
            throw new BusinessRuleException("Status can only move forward: " + current.getLabel()
                    + " \u2192 " + (current.next() == null ? "nothing (final status)" : current.next().getLabel())
                    + ". Cannot change to " + targetStatus.getLabel() + ".");
        }
        if (targetStatus == OrderStatus.PACKED) {
            requirePrintedShortfallUnits(order);
        }
        if (targetStatus == OrderStatus.SHIPPED) {
            String tracking = trimToNull(trackingId) != null ? trimToNull(trackingId) : order.getTrackingId();
            if (!StringUtils.hasText(tracking)) {
                throw new BusinessRuleException("A tracking id is required before marking the order as shipped.");
            }
            order.setTrackingId(tracking.trim());
        } else if (StringUtils.hasText(trackingId)) {
            order.setTrackingId(trackingId.trim());
        }

        order.setOrderStatus(targetStatus);
        // Packing is when the goods physically leave the shelf. Later statuses are covered too so
        // orders that were already past packing before this rule existed still settle their units.
        if (targetStatus.ordinal() >= OrderStatus.PACKED.ordinal()) {
            releaseShortfallStock(order);
        }
        return toView(orderRepository.save(order));
    }

    /**
     * Records what the courier just said about this order's parcel and, when the parcel has been
     * handed over, moves the order to {@link OrderStatus#DELIVERED} by itself.
     * <p>
     * Only a {@link OrderStatus#SHIPPED} order is advanced: anything earlier has not left the shop
     * (so a stale waybill cannot skip the flow) and {@code DELIVERED} is already final.
     *
     * @return the refreshed order, never null
     */
    @Transactional
    public OrderView applyTrackingSnapshot(Long orderId, TrackingSnapshot snapshot) {
        Order order = getEntity(orderId);
        order.setTrackingCheckedAt(LocalDateTime.now());
        order.setDeliveryStatusText(snapshot.summary());

        if (snapshot.delivered()) {
            LocalDate deliveredOn = snapshot.deliveredAt() == null
                    ? LocalDate.now()
                    : snapshot.deliveredAt().toLocalDate();
            if (order.getOrderStatus() == OrderStatus.SHIPPED) {
                order.setOrderStatus(OrderStatus.DELIVERED);
                order.setDeliveredOn(deliveredOn);
                // Mirrors the manual move: anything the order still owed leaves the shelf now.
                releaseShortfallStock(order);
            } else if (order.getOrderStatus() == OrderStatus.DELIVERED && order.getDeliveredOn() == null) {
                order.setDeliveredOn(deliveredOn);
            }
        }
        return toView(orderRepository.save(order));
    }

    /**
     * Packing takes the units off the shelf, so everything the order still owes must physically
     * exist first. An item only owes units when a shortfall was booked at order time; those are
     * put into stock by the dashboard "Ordered" button. When that stock is not there the order
     * cannot be packed, which keeps the inventory figures honest.
     */
    private void requirePrintedShortfallUnits(Order order) {
        List<String> missing = new ArrayList<>();
        for (OrderItem item : order.getOrderItems()) {
            int shortfall = Math.min(item.getShortfallQuantityOrZero(), item.getQuantity());
            if (shortfall == 0) {
                continue;
            }
            Product product = item.getProduct();
            int available = Math.max(0, product.getQuantityInHand());
            if (available < shortfall) {
                missing.add(product.getName() + " (" + (shortfall - available) + " of " + shortfall + ")");
            }
        }
        if (!missing.isEmpty()) {
            throw new BusinessRuleException("Cannot pack order #" + order.getId()
                    + " - these units are still to be printed: " + String.join(", ", missing)
                    + ". Press \"Ordered\" on the dashboard once they are printed, then pack the order.");
        }
    }

    /**
     * The shortfall units of an order were never deducted from stock - there was none left when
     * the order was placed. They are printed later via the dashboard "Ordered" button, which puts
     * them into stock. Packing the order takes those units off the shelf, so they are deducted
     * here and the booked shortfall is cleared, which also makes this safe to call again on every
     * later status change.
     */
    private void releaseShortfallStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            int shortfall = Math.min(item.getShortfallQuantityOrZero(), item.getQuantity());
            if (shortfall == 0) {
                continue;
            }
            Product product = item.getProduct();
            product.setQuantityInHand(Math.max(0, product.getQuantityInHand() - shortfall));
            product.setQuantityToBeOrdered(Math.max(0, product.getQuantityToBeOrdered() - shortfall));
            productRepository.save(product);
            item.setShortfallQuantity(0);
        }
    }

    /**
     * Deletes an order and puts the ordered quantities back into stock. The income line booked
     * for the order goes with it, otherwise the money book would keep a sale that never happened.
     */
    @Transactional
    public void delete(Long orderId) {
        Order order = getEntity(orderId);
        restoreStock(order);
        transactionService.removeOrderIncome(orderId);
        orderRepository.delete(order);
    }

    /* ---------------------------------------------------------------- mapping */

    private Map<Long, Integer> mergeItems(List<OrderItemForm> items) {
        Map<Long, Integer> merged = new LinkedHashMap<>();
        if (items == null) {
            return merged;
        }
        for (OrderItemForm item : items) {
            if (item == null || item.getProductId() == null || item.getQuantity() == null || item.getQuantity() < 1) {
                continue;
            }
            merged.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return merged;
    }

    public static OrderView toView(Order order) {
        List<OrderItemView> items = order.getOrderItems().stream()
                .map(i -> {
                    int shortfall = Math.min(i.getShortfallQuantityOrZero(), i.getQuantity());
                    int inHand = Math.max(0, i.getProduct().getQuantityInHand());
                    return OrderItemView.builder()
                            .productId(i.getProduct().getId())
                            .productName(i.getProduct().getName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getUnitPrice())
                            .lineTotal(i.getLineTotal())
                            .shortfallQuantity(shortfall)
                            .pendingPrintQuantity(Math.max(0, shortfall - inHand))
                            .build();
                })
                .toList();

        Customer customer = order.getCustomer();
        return OrderView.builder()
                .id(order.getId())
                .customerId(customer == null ? null : customer.getId())
                .customerName(customer == null ? "-" : customer.getName())
                .customerMobileNumber(customer == null ? "" : customer.getMobileNumber())
                .customerAddress(customer == null ? "" : customer.getAddress())
                .orderDate(order.getOrderDate())
                .orderStatus(order.getOrderStatus())
                .trackingId(order.getTrackingId())
                .deliveryStatusText(order.getDeliveryStatusText())
                .trackingCheckedAt(order.getTrackingCheckedAt())
                .deliveredOn(order.getDeliveredOn())
                .customizationNotes(order.getCustomizationNotes())
                .specialNotes(order.getSpecialNotes())
                .items(new ArrayList<>(items))
                .totalAmount(order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount())
                .totalQuantity(order.getTotalQuantity())
                .build();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

