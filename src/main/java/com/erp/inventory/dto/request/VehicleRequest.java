package com.erp.inventory.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequest {

    private Long companyId;

    @NotBlank(message = "Vehicle name is required")
    @Size(max = 40, message = "Vehicle name must be at most 40 characters")
    private String name;

    @Size(max = 120, message = "Owner name must be at most 120 characters")
    private String ownerName;

    @Size(max = 30, message = "Mobile no must be at most 30 characters")
    private String mobileNo;

    @Size(max = 120, message = "Contact person must be at most 120 characters")
    private String contactPerson;

    @Size(max = 30, message = "Contact mobile must be at most 30 characters")
    private String contactMobile;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @DecimalMin(value = "0.0", message = "TDS % cannot be negative")
    @DecimalMax(value = "100.0", message = "TDS % cannot exceed 100")
    private BigDecimal tdsPct;

    @PositiveOrZero(message = "Loading charge cannot be negative")
    private BigDecimal loadingCharge;

    private Boolean closed;

    private Map<String, Object> attributes;
}
