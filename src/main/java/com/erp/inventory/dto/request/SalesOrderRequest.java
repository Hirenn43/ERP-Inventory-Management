package com.erp.inventory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SalesOrderRequest {

    @NotNull(message = "Customer id is required")
    private Long customerId;

    @NotNull(message = "Warehouse id is required")
    private Long warehouseId;

    @NotEmpty(message = "Sales order must contain at least one item")
    @Valid
    private List<SalesOrderItemRequest> items;
}
