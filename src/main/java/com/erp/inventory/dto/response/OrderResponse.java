package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private String partyName;
    private String productName;
    private Long componentId;
    private LocalDate date;
    private BigDecimal rate;
    private BigDecimal orderedQty;
    private Map<String, Object> variant;

    // Convenience roll-ups derived from fulfillmentEvents, computed by the service - not stored columns.
    private BigDecimal completedQty;
    private BigDecimal dispatchedQty;
    private BigDecimal settledQty;
    private BigDecimal returnedQty;

    /** Units still to be produced: max(0, orderedQty − completedQty). Computed. */
    private BigDecimal pendingQty;

    /** Derived lifecycle bucket: "COMPLETED" when dispatchedQty ≥ orderedQty AND settledQty ≥ orderedQty,
     *  otherwise "PENDING". Never stored as a column. */
    private String status;

    /** Yarn this order consumes vs current stock, from the product BOM. Computed on read. */
    private OrderMaterialRequirementResponse materialRequirement;
    private boolean materialShortfall;

    /** Non-blocking advisories raised when the order was created (e.g. yarn stock shortfall). */
    private List<String> warnings;

    private List<OrderFulfillmentEventResponse> fulfillmentEvents;
    private String createdBy;
    private LocalDateTime createdAt;
}
