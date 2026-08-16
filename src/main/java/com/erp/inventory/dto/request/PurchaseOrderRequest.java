package com.erp.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PurchaseOrderRequest {

    @NotNull(message = "Supplier id is required")
    private Long supplierId;

    @NotNull(message = "Warehouse id is required")
    private Long warehouseId;

    @NotEmpty(message = "Purchase order must contain at least one item")
    @Valid
    private List<PurchaseOrderItemRequest> items;
}
