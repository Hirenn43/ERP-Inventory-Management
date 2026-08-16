package com.erp.inventory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerRequest {

    @NotBlank(message = "Customer name is required")
    private String name;

    private String phone;

    @Email(message = "Email must be valid")
    private String email;

    private String address;
}
