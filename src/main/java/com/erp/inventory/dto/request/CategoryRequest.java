package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    // Optional freeform fields, e.g. {"warrantyMonths": 12, "hazardous": false}
    private Map<String, Object> attributes;
}
