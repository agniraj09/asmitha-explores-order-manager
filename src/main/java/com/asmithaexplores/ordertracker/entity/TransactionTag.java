package com.asmithaexplores.ordertracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Free label used to segregate money by what it was for - "Flash Cards", "Banners",
 * "Courier" ... Maintained by the owner from the Finance &gt; Tags screen.
 */
@Entity
@Table(name = "transaction_tags", uniqueConstraints = @UniqueConstraint(
        name = "uk_transaction_tags_name", columnNames = "name"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    /** Hex colour (#rrggbb) used for the pill in the UI. */
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Column(length = 200)
    private String description;
}
