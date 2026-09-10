package com.erp.inventory.entity;

import com.erp.inventory.entity.enums.FieldDataType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A tenant-defined "sub-detail variable" for a master (e.g. a Parties field "Region").
 * Drives which inputs the frontend Details form shows and how {@code AttributeValidator}
 * checks the {@code attributes} JSONB bag on write.
 */
@Entity
@Table(name = "custom_field_definitions",
        uniqueConstraints = @UniqueConstraint(name = "uk_cfd_company_resource_key",
                columnNames = {"company_id", "resource", "field_key"}))
@Filter(name = "tenantFilter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomFieldDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    @ColumnDefault("0")
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    /** The master this field belongs to, matching the frontend CrudPage resource-key (e.g. "parties"). */
    @Column(length = 60, nullable = false)
    private String resource;

    /** The key stored in the owning entity's {@code attributes} map. */
    @Column(name = "field_key", length = 60, nullable = false)
    private String fieldKey;

    @Column(length = 120, nullable = false)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 20, nullable = false)
    private FieldDataType dataType;

    /** Max characters for TEXT / SELECT. */
    @Column(name = "max_length")
    private Integer maxLength;

    @Column(name = "min_value", precision = 18, scale = 4)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 18, scale = 4)
    private BigDecimal maxValue;

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean required = false;

    /** The value must be unique among this tenant's rows of {@code resource}. */
    @Builder.Default
    @Column(name = "unique_value", nullable = false)
    @ColumnDefault("false")
    private boolean uniqueValue = false;

    /** Optional section heading the field is grouped under in the Details form. */
    @Column(length = 60)
    private String section;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    @ColumnDefault("0")
    private int displayOrder = 0;

    /** Allowed values when {@code dataType == SELECT}. */
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> options = new ArrayList<>();

    @Builder.Default
    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean active = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
