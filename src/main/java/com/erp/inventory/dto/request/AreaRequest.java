package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AreaRequest {

    private Long companyId;

    @NotBlank(message = "Area name is required")
    @Size(max = 120, message = "Area name must be at most 120 characters")
    private String name;

    @Size(max = 120, message = "Contact mobile must be at most 120 characters")
    private String contactMobile;

    @PositiveOrZero(message = "Rate cannot be negative")
    private BigDecimal rate;

    private Map<String, Object> attributes;
}
