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
public class JobRequest {

    private Long companyId;

    @NotBlank(message = "Job name is required")
    @Size(max = 120, message = "Job name must be at most 120 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @PositiveOrZero(message = "Rate cannot be negative")
    private BigDecimal rate;

    private Boolean finishedGoodsType;

    private Boolean defaultRepeatIssue;

    private Map<String, Object> attributes;
}
