package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class CompanyRequest {

    @NotBlank(message = "Company name is required")
    private String name;

    private String address;
    private String mobile;
    private Map<String, Object> attributes;
}
