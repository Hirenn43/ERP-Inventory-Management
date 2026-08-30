package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class OrderRequest {

    @NotNull(message = "Party id is required")
    private Long partyId;

    @NotNull(message = "Product id is required")
    private Long productId;

    // Which BOM/matching variant of the product, if applicable.
    private Long componentId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private BigDecimal rate;

    @NotNull(message = "Ordered quantity is required")
    @Positive(message = "Ordered quantity must be greater than zero")
    private BigDecimal orderedQty;

    // Order-specific variant details, e.g. {"groundColor": "Red-150-POLY1", "palluColor": "..."}
    private Map<String, Object> variant;
}
