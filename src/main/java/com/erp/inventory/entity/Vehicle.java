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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A vehicle (by registration number) used for dispatch, with TDS and loading-charge defaults. */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(length = 40, nullable = false)
    private String name;

    @Column(length = 120)
    private String ownerName;

    @Column(length = 30)
    private String mobileNo;

    @Column(length = 120)
    private String contactPerson;

    @Column(length = 30)
    private String contactMobile;

    @Column(length = 255)
    private String description;

    @Column(precision = 6, scale = 2)
    private BigDecimal tdsPct;

    @Column(precision = 12, scale = 2)
    private BigDecimal loadingCharge;

    @Column
    private Boolean closed;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
