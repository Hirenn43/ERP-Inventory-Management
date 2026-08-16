package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private Long id;
    private Long productId;
    private String productName;
    private String sku;
    private Long warehouseId;
    private String warehouseName;
    private Integer quantity;
}
