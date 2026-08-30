package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/** How much of each raw material (yarn) an order for {@code orderedQty} units of a product will consume,
 *  set against current on-hand stock. Computed on read from the product's ProductComponent BOM and the
 *  MaterialStockMovement ledger - nothing here is stored. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderMaterialRequirementResponse {

    private Long productId;
    private BigDecimal orderedQty;
    private List<Line> lines;
    /** True when any line's requiredQty exceeds its availableQty. */
    private boolean anyShortfall;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String slotNo;
        private Long materialId;
        private String materialCode;
        private String unit;
        /** Material consumed per unit of product. Null when the BOM row leaves it unspecified. */
        private BigDecimal quantityPerUnit;
        private BigDecimal requiredQty;
        private BigDecimal availableQty;
        /** max(0, requiredQty − availableQty). */
        private BigDecimal shortfallQty;
        private boolean sufficient;
        /** True when quantityPerUnit was not set on the BOM row, so requiredQty could not be computed. */
        private boolean quantityPerUnitMissing;
    }
}
