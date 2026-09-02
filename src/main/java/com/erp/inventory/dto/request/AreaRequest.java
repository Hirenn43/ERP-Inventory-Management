package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AreaRequest {

    private Long companyId;

    @NotBlank(message = "Area name is required")
    private String name;

    private String contactMobile;

    private BigDecimal rate;

    private Map<String, Object> attributes;
}
