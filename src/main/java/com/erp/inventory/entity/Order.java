package com.erp.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A finished-goods order. Fulfillment is NOT a single status column - it's tracked as a ledger of
 *  OrderFulfillmentEvent rows (ordered qty is here; completed/dispatched/settled/returned qty each
 *  accumulate independently in the events, since real orders fulfill and return in partial batches). */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id", nullable = false)
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** Which BOM/matching variant of the product was selected for this order, if the product has more
     *  than one ProductComponent set. Optional. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private ProductComponent component;

    @Column(nullable = false)
    private LocalDate date;

    @Column(precision = 12, scale = 2)
    private BigDecimal rate;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal orderedQty;

    // Order-specific variant details that don't need their own columns (e.g. ground color, pallu color).
    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> variant = new HashMap<>();

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderFulfillmentEvent> fulfillmentEvents = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void addFulfillmentEvent(OrderFulfillmentEvent event) {
        fulfillmentEvents.add(event);
        event.setOrder(this);
    }
}
