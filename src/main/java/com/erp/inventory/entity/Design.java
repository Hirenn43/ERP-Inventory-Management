package com.erp.inventory.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A design/pattern that a finished product is produced against. */
@Entity
@Table(name = "designs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Design {

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

    @Column(length = 120, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 60)
    private String stitch;

    @Column(precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(length = 80)
    private String designGroup;

    @Column
    private Boolean closed;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
