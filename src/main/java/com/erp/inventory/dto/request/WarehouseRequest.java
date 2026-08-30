package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class WarehouseRequest {

    @NotBlank(message = "Warehouse name is required")
    private String name;

    private String location;

    private Map<String, Object> attributes;
}
