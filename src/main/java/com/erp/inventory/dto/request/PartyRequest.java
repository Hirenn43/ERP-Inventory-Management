package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.PartyRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class PartyRequest {

    private Long companyId;

    @NotBlank(message = "Party name is required")
    private String name;

    private String address;
    private String mobile;

    @Email(message = "Email must be valid")
    private String email;

    @NotEmpty(message = "At least one role is required")
    private Set<PartyRole> roles;

    private Map<String, Object> attributes;
}
