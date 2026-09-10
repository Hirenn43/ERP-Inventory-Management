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

/** A commission agent / broker who introduces parties and earns brokerage on their trade. */
@Entity
@Table(name = "brokers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Broker {

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

    @Column(length = 150, nullable = false)
    private String name;

    @Column(length = 255)
    private String address1;

    @Column(length = 255)
    private String address2;

    @Column(length = 255)
    private String address3;

    @Column(length = 255)
    private String address4;

    @Column(length = 30)
    private String phone1;

    @Column(length = 120)
    private String contactPerson1;

    @Column(length = 30)
    private String phone2;

    @Column(length = 120)
    private String contactPerson2;

    @Column(length = 120)
    private String email;

    @Column(precision = 6, scale = 2)
    private BigDecimal brokeragePct;

    @Column
    private Boolean blacklisted;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
