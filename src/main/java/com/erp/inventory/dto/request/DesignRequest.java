package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DesignRequest {

    private Long companyId;

    @NotBlank(message = "Design name is required")
    private String name;

    private String description;

    private String stitch;

    private BigDecimal rate;

    private String designGroup;

    private Boolean closed;

    private Map<String, Object> attributes;
}
