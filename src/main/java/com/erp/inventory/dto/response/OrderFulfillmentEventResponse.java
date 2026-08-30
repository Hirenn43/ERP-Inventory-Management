package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.FulfillmentStage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderFulfillmentEventResponse {
    private Long id;
    private FulfillmentStage stage;
    private BigDecimal quantity;
    private LocalDate date;
    private String resourceName;
    private String remarks;
}
