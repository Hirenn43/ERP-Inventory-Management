package com.erp.inventory.entity;

import com.erp.inventory.entity.enums.MovementDirection;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/** Raw-material purchase/sale ledger - replaces the old PurchaseOrder/SalesOrder pair for materials.
 *  IN = purchased from the party, OUT = sold to the party. */
@Entity
@Table(name = "material_stock_movements", indexes = {
        // On-hand is Σ over the whole ledger for a material on every read (sumSignedQuantityByMaterialId,
        // sumSignedQuantityGrouped, findByMaterialIdOrderByDateDesc). Postgres does not auto-index FKs.
        @Index(name = "idx_msm_material_date", columnList = "material_id, date"),
        @Index(name = "idx_msm_party", columnList = "party_id")
})
@Filter(name = "tenantFilter", condition = "material_id in (select m.id from materials m where m.company_id = :companyId)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialStockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MovementDirection direction;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal quantity;

    @Column(length = 60)
    private String invoiceNo;

    @Column(length = 60)
    private String lotNo;

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
