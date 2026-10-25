package com.asmithaexplores.ordertracker.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> orderItems = new ArrayList<>();

    /** Free text instructions from the customer - mapped to a PostgreSQL "text" column. */
    @Column(name = "customization_notes", columnDefinition = "text")
    private String customizationNotes;

    /** Anything the owner has to keep in mind for this order: urgency, packing, payment ... */
    @Column(name = "special_notes", columnDefinition = "text")
    private String specialNotes;

    @Builder.Default
    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate = LocalDate.now();

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.NEW;

    @Column(name = "tracking_id", length = 60)
    private String trackingId;

    /** Latest wording from the courier, e.g. "Out for Delivery". Refreshed by the delivery sync. */
    @Column(name = "delivery_status_text", length = 120)
    private String deliveryStatusText;

    /** When the courier last answered for this parcel - {@code null} means never looked up. */
    @Column(name = "tracking_checked_at")
    private LocalDateTime trackingCheckedAt;

    /** Delivery date reported by the courier, set when the order is auto-moved to DELIVERED. */
    @Column(name = "delivered_on")
    private LocalDate deliveredOn;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        orderItems.add(item);
    }

    /** Sum of all line totals. Never stored - always derived from the items. */
    public BigDecimal getTotalAmount() {
        return orderItems.stream()
                .map(OrderItem::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int getTotalQuantity() {
        return orderItems.stream().mapToInt(OrderItem::getQuantity).sum();
    }
}

