package com.erp.inventory.entity;

import com.erp.inventory.entity.enums.ScheduleEffect;
import com.erp.inventory.entity.enums.ScheduleType;
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

/** An accounting group (chart-of-accounts head) a party's ledger rolls up into, e.g. SUNDRY DEBTORS FOR GOODS. */
@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

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

    @Column
    private Long parentId;

    @Column
    private Boolean subGroupRequired;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ScheduleEffect effectOn;

    @Column
    private Integer scheduleIndex;

    @Column
    private Boolean effectOnGp;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ScheduleType scheduleType;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> attributes = new HashMap<>();
}
