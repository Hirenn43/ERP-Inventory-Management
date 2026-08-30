package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.FulfillmentStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class OrderFulfillmentEventRequest {

    @NotNull(message = "Stage is required")
    private FulfillmentStage stage;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "Date is required")
    private LocalDate date;

    private Long resourceId;
    private String remarks;
}
