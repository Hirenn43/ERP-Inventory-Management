package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.StockType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class MaterialRequest {

    private Long companyId;

    @NotBlank(message = "Material code is required")
    private String code;

    private String quality;
    private String unit;

    private String name;
    private String hsnCode;
    private String uom;
    private StockType stockType;
    private BigDecimal gstRate;
    private Long categoryId;
    private Boolean closed;

    // Optional freeform fields, e.g. {"denier": 150}
    private Map<String, Object> attributes;
}
