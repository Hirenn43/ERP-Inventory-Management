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
public class ProductComponentResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String slotNo;
    private Long materialId;
    private String materialCode;
    private BigDecimal quantityPerUnit;
}
