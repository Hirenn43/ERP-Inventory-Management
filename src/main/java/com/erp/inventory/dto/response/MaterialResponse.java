package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaterialResponse {
    private Long id;
    private Long companyId;
    private String code;
    private String quality;
    private String unit;
    /** On-hand quantity: Σ(IN) − Σ(OUT) over this material's stock movements. Computed, never stored. */
    private BigDecimal availableQty;
    private Map<String, Object> attributes;
}
