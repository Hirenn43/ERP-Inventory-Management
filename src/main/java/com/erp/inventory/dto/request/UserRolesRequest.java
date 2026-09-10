package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserRolesRequest {
    @NotEmpty(message = "At least one role is required")
    private Set<RoleName> roles;
}
