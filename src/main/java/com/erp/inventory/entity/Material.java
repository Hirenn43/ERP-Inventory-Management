package com.erp.inventory.entity;

import com.erp.inventory.entity.enums.StockType;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.Map;

/** A raw material / input variant (e.g. "GOLD-150-POLY1" yarn) - distinct from Product, which is what
 *  gets sold. Purchased and consumed rather than designed and sold. */
@Entity
@Table(name = "materials", indexes = {
        @Index(name = "idx_materials_company", columnList = "company_id"),
        @Index(name = "idx_materials_category", columnList = "category_id")
})
@Filter(name = "tenantFilter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 80, unique = true)
    private String code;

    @Column(length = 80)
    private String quality;

    @Column(length = 20)
    private String unit;

    // --- Item Master fields ---------------------------------------------
    @Column(length = 150)
    private String name;

    @Column(length = 20)
    private String hsnCode;

    @Column(length = 20)
    private String uom;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StockType stockType;

    @Column(precision = 6, scale = 2)
    private BigDecimal gstRate;

    /** Item Group. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private Boolean closed;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
