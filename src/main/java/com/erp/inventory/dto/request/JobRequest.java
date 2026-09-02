package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobRequest {

    private Long companyId;

    @NotBlank(message = "Job name is required")
    private String name;

    private String description;

    private BigDecimal rate;

    private Boolean finishedGoodsType;

    private Boolean defaultRepeatIssue;

    private Map<String, Object> attributes;
}
