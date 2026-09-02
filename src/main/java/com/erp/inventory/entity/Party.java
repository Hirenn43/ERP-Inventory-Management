package com.erp.inventory.entity;

import com.erp.inventory.entity.enums.PartyRole;
import com.erp.inventory.entity.enums.RegistrationType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Replaces separate Customer/Supplier tables: one party can hold any combination of roles at once. */
@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String address;

    @Column(length = 20)
    private String mobile;

    @Column(length = 100)
    private String email;

    // --- Account Master: GST / registration -------------------------------
    @Column(length = 20)
    private String gstin;

    @Column(length = 15)
    private String pan;

    @Column(length = 2)
    private String stateCode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RegistrationType registrationType;

    // --- Account Master: address extras ----------------------------------
    @Column(length = 80)
    private String city;

    @Column(length = 80)
    private String state;

    @Column(length = 12)
    private String pinCode;

    @Column(length = 120)
    private String contactPersonName;

    // --- Account Master: ledger / limits --------------------------------
    @Column(precision = 14, scale = 2)
    private BigDecimal openingBalance;

    @Column(precision = 14, scale = 2)
    private BigDecimal creditLimitAmount;

    private Integer creditLimitDays;

    private Boolean blacklisted;

    /** Account Master "Party Close" - kept for history, hidden from pickers. */
    private Boolean closed;

    // --- Account Master: master-data links (plain ids, resolved client-side) ---
    private Long scheduleId;
    private Long brokerId;
    private Long areaId;

    @Builder.Default
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "party_roles", joinColumns = @JoinColumn(name = "party_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private Set<PartyRole> roles = new HashSet<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
