package com.asmithaexplores.ordertracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One line in the money book: either income (normally the value of an order) or an expense the
 * owner entered by hand - printing, packing, shipping, transport ...
 * <p>
 * Income lines are created automatically when an order is placed and stay linked to it, so they
 * follow the order when it is edited or deleted. The owner can still correct the amount by hand;
 * that marks the line as {@link #manuallyAdjusted} and the automatic sync leaves it alone from
 * then on.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Always kept equal to {@code category.getType()} - see {@link #setCategory(TransactionCategory)}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransactionCategory category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate = LocalDate.now();

    @Column(columnDefinition = "text")
    private String description;

    /** Set only on the income line that belongs to an order. {@code null} for manual entries. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id")
    private Order order;

    /** {@code true} when the line was created by the application rather than typed in. */
    @Builder.Default
    @Column(name = "system_generated", nullable = false)
    private boolean systemGenerated = false;

    /** {@code true} once the owner edited a system generated line - stops the automatic sync. */
    @Builder.Default
    @Column(name = "manually_adjusted", nullable = false)
    private boolean manuallyAdjusted = false;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "transaction_tag_links",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    @OrderBy("name asc")
    private Set<TransactionTag> tags = new LinkedHashSet<>();

    @Builder.Default
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    /** Setting the category also fixes the direction of the money, so the two can never drift. */
    public void setCategory(TransactionCategory category) {
        this.category = category;
        this.type = category == null ? null : category.getType();
    }

    /** Safety net for instances built through the Lombok builder, which bypasses the setter. */
    @PrePersist
    @PreUpdate
    void deriveTypeFromCategory() {
        this.type = category == null ? null : category.getType();
    }

    /** The amount signed by direction: positive for income, negative for an expense. */
    public BigDecimal getSignedAmount() {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        return type == TransactionType.EXPENSE ? value.negate() : value;
    }
}
