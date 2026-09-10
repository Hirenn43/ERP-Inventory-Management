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
public class DesignRequest {

    private Long companyId;

    @NotBlank(message = "Design name is required")
    @Size(max = 120, message = "Design name must be at most 120 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Size(max = 60, message = "Stitch must be at most 60 characters")
    private String stitch;

    @PositiveOrZero(message = "Rate cannot be negative")
    private BigDecimal rate;

    @Size(max = 80, message = "Design group must be at most 80 characters")
    private String designGroup;

    private Boolean closed;

    private Map<String, Object> attributes;
}
