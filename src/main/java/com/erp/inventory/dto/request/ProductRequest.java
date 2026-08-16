package com.erp.inventory.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductRequest {

    @NotBlank(message = "SKU is required")
    private String sku;

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @NotNull(message = "Cost price is required")
    @DecimalMin(value = "0.0", message = "Cost price must be non-negative")
    private BigDecimal costPrice;

    @PositiveOrZero(message = "Reorder level must be non-negative")
    private Integer reorderLevel;

    private Long categoryId;

    private Long supplierId;
}
