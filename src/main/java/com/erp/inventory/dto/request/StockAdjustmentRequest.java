package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockAdjustmentRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotNull(message = "Warehouse id is required")
    private Long warehouseId;

    @NotNull(message = "Movement type is required")
    private MovementType type;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private Integer quantity;

    private String remarks;
}
