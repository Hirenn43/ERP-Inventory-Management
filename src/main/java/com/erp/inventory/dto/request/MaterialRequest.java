package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.StockType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class MaterialRequest {

    private Long companyId;

    @NotBlank(message = "Material code is required")
    @Size(max = 80, message = "Material code must be at most 80 characters")
    private String code;

    @Size(max = 80, message = "Quality must be at most 80 characters")
    private String quality;

    @Size(max = 20, message = "Unit must be at most 20 characters")
    private String unit;

    @Size(max = 150, message = "Name must be at most 150 characters")
    private String name;

    @Size(max = 20, message = "HSN code must be at most 20 characters")
    private String hsnCode;

    @Size(max = 20, message = "UOM must be at most 20 characters")
    private String uom;

    private StockType stockType;

    @DecimalMin(value = "0.0", message = "GST rate cannot be negative")
    @DecimalMax(value = "100.0", message = "GST rate cannot exceed 100")
    private BigDecimal gstRate;

    private Long categoryId;
    private Boolean closed;

    // Optional freeform fields, e.g. {"denier": 150}
    private Map<String, Object> attributes;
}
