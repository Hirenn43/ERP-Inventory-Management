package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.TransportMode;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransporterRequest {

    private Long companyId;

    @NotBlank(message = "Transporter name is required")
    private String name;

    private String address1;

    private String address2;

    private String address3;

    private String address4;

    private String phone;

    private String contactPerson;

    private String gstin;

    private TransportMode transportMode;

    private Map<String, Object> attributes;
}
