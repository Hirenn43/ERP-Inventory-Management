package com.erp.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Bill-of-materials line: which Material fills which slot of a Product. Generalizes the yarn app's
 *  per-design "Matching" table (feeder -> color code). */
@Entity
@Table(name = "product_components")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 40)
    private String slotNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    /** How much of this material one unit of the product consumes; optional. */
    @Column(precision = 12, scale = 4)
    private BigDecimal quantityPerUnit;
}
