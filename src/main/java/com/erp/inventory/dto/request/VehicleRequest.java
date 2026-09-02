package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequest {

    private Long companyId;

    @NotBlank(message = "Vehicle name is required")
    private String name;

    private String ownerName;

    private String mobileNo;

    private String contactPerson;

    private String contactMobile;

    private String description;

    private BigDecimal tdsPct;

    private BigDecimal loadingCharge;

    private Boolean closed;

    private Map<String, Object> attributes;
}
