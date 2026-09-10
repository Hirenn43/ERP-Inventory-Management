package com.erp.inventory.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrokerRequest {

    private Long companyId;

    @NotBlank(message = "Broker name is required")
    @Size(max = 150, message = "Broker name must be at most 150 characters")
    private String name;

    @Size(max = 255, message = "Address line 1 must be at most 255 characters")
    private String address1;

    @Size(max = 255, message = "Address line 2 must be at most 255 characters")
    private String address2;

    @Size(max = 255, message = "Address line 3 must be at most 255 characters")
    private String address3;

    @Size(max = 255, message = "Address line 4 must be at most 255 characters")
    private String address4;

    @Size(max = 30, message = "Phone 1 must be at most 30 characters")
    private String phone1;

    @Size(max = 120, message = "Contact person 1 must be at most 120 characters")
    private String contactPerson1;

    @Size(max = 30, message = "Phone 2 must be at most 30 characters")
    private String phone2;

    @Size(max = 120, message = "Contact person 2 must be at most 120 characters")
    private String contactPerson2;

    @Email(message = "Email must be valid")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    @DecimalMin(value = "0.0", message = "Brokerage % cannot be negative")
    @DecimalMax(value = "100.0", message = "Brokerage % cannot exceed 100")
    private BigDecimal brokeragePct;

    private Boolean blacklisted;

    private Map<String, Object> attributes;
}
