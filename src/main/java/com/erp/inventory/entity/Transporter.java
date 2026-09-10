package com.erp.inventory.entity;

import com.erp.inventory.entity.enums.TransportMode;
import jakarta.persistence.*;
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

/** A transport company used on dispatch / e-way bills. */
@Entity
@Table(name = "transporters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transporter {

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
    private String phone;

    @Column(length = 120)
    private String contactPerson;

    @Column(length = 20)
    private String gstin;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private TransportMode transportMode;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
