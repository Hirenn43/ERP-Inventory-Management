package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.MovementDirection;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
public class MaterialStockMovementRequest {

    @NotNull(message = "Material id is required")
    private Long materialId;

    @NotNull(message = "Party id is required")
    private Long partyId;

    @NotNull(message = "Direction is required")
    private MovementDirection direction;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    private String invoiceNo;
    private String lotNo;

    @DecimalMin(value = "0.0", message = "Price must be non-negative")
    private BigDecimal price;

    private Map<String, Object> attributes;
}
