package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.PartyRole;
import com.erp.inventory.entity.enums.RegistrationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class PartyRequest {

    private Long companyId;

    @NotBlank(message = "Party name is required")
    @Size(max = 150, message = "Party name must be at most 150 characters")
    private String name;

    @Size(max = 255, message = "Address must be at most 255 characters")
    private String address;

    @Size(max = 20, message = "Mobile must be at most 20 characters")
    private String mobile;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @NotEmpty(message = "At least one role is required")
    private Set<PartyRole> roles;

    @Size(max = 20, message = "GSTIN must be at most 20 characters")
    private String gstin;

    @Size(max = 15, message = "PAN must be at most 15 characters")
    private String pan;

    @Size(max = 2, message = "State code must be at most 2 characters")
    private String stateCode;

    private RegistrationType registrationType;

    @Size(max = 80, message = "City must be at most 80 characters")
    private String city;

    @Size(max = 80, message = "State must be at most 80 characters")
    private String state;

    @Size(max = 12, message = "Pin code must be at most 12 characters")
    private String pinCode;

    @Size(max = 120, message = "Contact person name must be at most 120 characters")
    private String contactPersonName;

    private BigDecimal openingBalance;

    @PositiveOrZero(message = "Credit limit amount cannot be negative")
    private BigDecimal creditLimitAmount;

    @PositiveOrZero(message = "Credit limit days cannot be negative")
    private Integer creditLimitDays;

    private Boolean blacklisted;
    private Boolean closed;
    private Long scheduleId;
    private Long brokerId;
    private Long areaId;

    private Map<String, Object> attributes;
}
