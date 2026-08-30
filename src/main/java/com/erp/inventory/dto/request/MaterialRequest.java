package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class MaterialRequest {

    private Long companyId;

    @NotBlank(message = "Material code is required")
    private String code;

    private String quality;
    private String unit;

    // Optional freeform fields, e.g. {"denier": 150}
    private Map<String, Object> attributes;
}
