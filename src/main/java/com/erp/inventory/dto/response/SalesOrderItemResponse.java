package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesOrderItemResponse {
    private Long productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
}
