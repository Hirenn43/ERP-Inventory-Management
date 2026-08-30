package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductComponentRequest {

    @NotNull(message = "Product id is required")
    private Long productId;

    @NotBlank(message = "Slot number is required")
    private String slotNo;

    @NotNull(message = "Material id is required")
    private Long materialId;

    private BigDecimal quantityPerUnit;
}
