package com.asmithaexplores.ordertracker.service;

import com.asmithaexplores.ordertracker.dto.TransactionFilter;
import com.asmithaexplores.ordertracker.dto.TransactionForm;
import com.asmithaexplores.ordertracker.dto.TransactionSummary;
import com.asmithaexplores.ordertracker.dto.TransactionTagView;
import com.asmithaexplores.ordertracker.dto.TransactionView;
import com.asmithaexplores.ordertracker.entity.Order;
import com.asmithaexplores.ordertracker.entity.OrderItem;
import com.asmithaexplores.ordertracker.entity.Transaction;
import com.asmithaexplores.ordertracker.entity.TransactionCategory;
import com.asmithaexplores.ordertracker.entity.TransactionTag;
import com.asmithaexplores.ordertracker.entity.TransactionType;
import com.asmithaexplores.ordertracker.exception.BusinessRuleException;
import com.asmithaexplores.ordertracker.exception.ResourceNotFoundException;
import com.asmithaexplores.ordertracker.repository.OrderRepository;
import com.asmithaexplores.ordertracker.repository.TransactionRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The money book. Income lines mirror the orders that were placed, expenses - printing, packing,
 * shipping, transport - are entered by hand. Every line can carry tags so the owner can see how
 * much a product line such as flash cards or banners really costs.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final String UNTAGGED = "Untagged";

    private final TransactionRepository transactionRepository;
    private final TransactionTagService tagService;
    private final OrderRepository orderRepository;

    /* ------------------------------------------------------------------ reads */

    public List<TransactionView> search(TransactionFilter filter) {
        TransactionFilter f = filter == null ? new TransactionFilter() : filter;
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (f.getType() != null) {
                predicates.add(cb.equal(root.get("type"), f.getType()));
            }
            if (f.getCategory() != null) {
                predicates.add(cb.equal(root.get("category"), f.getCategory()));
            }
            if (f.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), f.getFromDate()));
            }
            if (f.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), f.getToDate()));
            }
            if (f.getTagId() != null) {
                // A transaction has many tags, so the join multiplies rows - keep one per line.
                predicates.add(cb.equal(root.join("tags").get("id"), f.getTagId()));
                if (query != null) {
                    query.distinct(true);
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec,
                        Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id")))
                .stream()
                .map(TransactionService::toView)
                .toList();
    }

    /** Totals for a list that is already on screen - no second trip to the database. */
    public TransactionSummary summarise(List<TransactionView> transactions) {
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        Map<String, BigDecimal> byCategory = new LinkedHashMap<>();
        Map<String, BigDecimal> byTag = new LinkedHashMap<>();

        for (TransactionView t : transactions) {
            BigDecimal amount = t.getAmount() == null ? BigDecimal.ZERO : t.getAmount();
            if (t.isIncome()) {
                income = income.add(amount);
                continue;
            }
            expense = expense.add(amount);
            byCategory.merge(t.getCategory().getLabel(), amount, BigDecimal::add);
            if (t.getTags().isEmpty()) {
                byTag.merge(UNTAGGED, amount, BigDecimal::add);
            } else {
                // A transaction can carry several tags; the full amount is counted under each of
                // them, so the tag breakdown shows exposure per tag and not a split of the total.
                t.getTags().forEach(tag -> byTag.merge(tag.getName(), amount, BigDecimal::add));
            }
        }

        return TransactionSummary.builder()
                .totalIncome(income)
                .totalExpense(expense)
                .transactionCount(transactions.size())
                .expenseByCategory(sortedByValueDesc(byCategory))
                .expenseByTag(sortedByValueDesc(byTag))
                .build();
    }

    /** Income, expense and profit between two dates - used by the dashboard. */
    public TransactionSummary summariseBetween(LocalDate from, LocalDate to) {
        Map<TransactionType, BigDecimal> totals = new LinkedHashMap<>();
        for (Object[] row : transactionRepository.sumByTypeBetween(from, to)) {
            totals.put((TransactionType) row[0],
                    row[1] == null ? BigDecimal.ZERO : new BigDecimal(row[1].toString()));
        }
        return TransactionSummary.builder()
                .totalIncome(totals.getOrDefault(TransactionType.INCOME, BigDecimal.ZERO))
                .totalExpense(totals.getOrDefault(TransactionType.EXPENSE, BigDecimal.ZERO))
                .build();
    }

    public Transaction getEntity(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction", id));
    }

    public TransactionView get(Long id) {
        return toView(getEntity(id));
    }

    public TransactionForm getForm(Long id) {
        Transaction transaction = getEntity(id);
        return TransactionForm.builder()
                .id(transaction.getId())
                .category(transaction.getCategory())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .description(transaction.getDescription())
                .orderId(transaction.getOrder() == null ? null : transaction.getOrder().getId())
                .tagIds(transaction.getTags().stream().map(TransactionTag::getId).toList())
                .build();
    }

    /* ----------------------------------------------------------------- writes */

    /**
     * Creates or updates a hand written line. Editing the automatic income line of an order marks
     * it as manually adjusted, so later edits to the order no longer overwrite the amount.
     */
    @Transactional
    public Transaction save(TransactionForm form) {
        Transaction transaction = form.getId() == null ? new Transaction() : getEntity(form.getId());

        if (form.getId() != null && transaction.isSystemGenerated()) {
            transaction.setManuallyAdjusted(true);
        }

        transaction.setCategory(form.getCategory());
        transaction.setAmount(form.getAmount());
        transaction.setTransactionDate(
                form.getTransactionDate() == null ? LocalDate.now() : form.getTransactionDate());
        transaction.setDescription(trimToNull(form.getDescription()));
        transaction.setTags(tagService.resolve(form.getTagIds()));

        return transactionRepository.save(transaction);
    }

    @Transactional
    public void delete(Long id) {
        Transaction transaction = getEntity(id);
        if (transaction.getOrder() != null) {
            throw new BusinessRuleException("This income belongs to order #" + transaction.getOrder().getId()
                    + ". Delete the order itself to remove it.");
        }
        transactionRepository.delete(transaction);
    }

    /* ------------------------------------------------------- order integration */

    /**
     * Books the value of a freshly placed order as income. The line is pre-tagged with the
     * product categories in the order - flash cards, banners ... - whenever a tag of that name
     * exists, so the money is segregated without any extra typing.
     */
    @Transactional
    public Transaction recordOrderIncome(Order order) {
        Transaction transaction = new Transaction();
        transaction.setOrder(order);
        transaction.setSystemGenerated(true);
        transaction.setCategory(TransactionCategory.ORDER_SALE);
        applyOrderValues(transaction, order);
        transaction.setTags(tagsForOrder(order));
        return transactionRepository.save(transaction);
    }

    /**
     * Keeps the income line in step with an order whose contents changed. A line the owner has
     * corrected by hand is left untouched - their figure wins.
     */
    @Transactional
    public void syncOrderIncome(Order order) {
        Optional<Transaction> existing = transactionRepository.findByOrderId(order.getId());
        if (existing.isEmpty()) {
            recordOrderIncome(order);
            return;
        }
        Transaction transaction = existing.get();
        if (transaction.isManuallyAdjusted()) {
            return;
        }
        applyOrderValues(transaction, order);
        transaction.setTags(tagsForOrder(order));
        transactionRepository.save(transaction);
    }

    /** Removes the income line of an order - called before the order itself is deleted. */
    @Transactional
    public void removeOrderIncome(Long orderId) {
        transactionRepository.deleteByOrderId(orderId);
    }

    /**
     * Books the missing income for orders that were placed before the money book existed, so the
     * Finance screens show the full history instead of starting from the day of the upgrade.
     * Runs on every start and is a no-op once every order has its line.
     *
     * @return how many income lines were created
     */
    @Transactional
    public int backfillMissingOrderIncome() {
        List<Order> orders = orderRepository.findWithoutIncomeTransaction();
        orders.forEach(this::recordOrderIncome);
        return orders.size();
    }

    private void applyOrderValues(Transaction transaction, Order order) {
        transaction.setAmount(order.getTotalAmount());
        transaction.setTransactionDate(order.getOrderDate());
        transaction.setDescription("Order #" + order.getId() + " · " + order.getCustomer().getName());
    }

    private Set<TransactionTag> tagsForOrder(Order order) {
        Set<String> categories = new LinkedHashSet<>();
        for (OrderItem item : order.getOrderItems()) {
            String category = item.getProduct().getCategory();
            if (category != null && !category.isBlank()) {
                categories.add(category);
            }
        }
        return tagService.findByNames(categories);
    }

    /* ---------------------------------------------------------------- mapping */

    public static TransactionView toView(Transaction transaction) {
        List<TransactionTagView> tags = transaction.getTags().stream()
                .map(TransactionTagService::toPill)
                .sorted(Comparator.comparing(TransactionTagView::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return TransactionView.builder()
                .id(transaction.getId())
                .type(transaction.getType())
                .category(transaction.getCategory())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .formattedDate(transaction.getTransactionDate() == null
                        ? "" : transaction.getTransactionDate().format(DATE_FORMAT))
                .description(transaction.getDescription())
                .orderId(transaction.getOrder() == null ? null : transaction.getOrder().getId())
                .customerName(transaction.getOrder() == null
                        ? null : transaction.getOrder().getCustomer().getName())
                .systemGenerated(transaction.isSystemGenerated())
                .manuallyAdjusted(transaction.isManuallyAdjusted())
                .tags(tags)
                .build();
    }

    private static Map<String, BigDecimal> sortedByValueDesc(Map<String, BigDecimal> source) {
        Map<String, BigDecimal> sorted = new LinkedHashMap<>();
        source.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
