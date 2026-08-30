package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WhatsappUserLinkRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "[+()\\d\\s-]{7,25}", message = "Phone must be a valid number (digits, spaces, +, -, ())")
    private String phone;

    @NotNull(message = "User id is required")
    private Long userId;

    private Long companyId;

    private Boolean enabled;
}
