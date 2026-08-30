package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class ResourceRequest {

    private Long companyId;

    @NotBlank(message = "Resource name is required")
    private String name;

    // Optional freeform fields, e.g. {"panna": 74}
    private Map<String, Object> attributes;
}
