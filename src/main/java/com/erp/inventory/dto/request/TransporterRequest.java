package com.erp.inventory.dto.request;

import com.erp.inventory.entity.enums.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransporterRequest {

    private Long companyId;

    @NotBlank(message = "Transporter name is required")
    @Size(max = 150, message = "Transporter name must be at most 150 characters")
    private String name;

    @Size(max = 255, message = "Address line 1 must be at most 255 characters")
    private String address1;

    @Size(max = 255, message = "Address line 2 must be at most 255 characters")
    private String address2;

    @Size(max = 255, message = "Address line 3 must be at most 255 characters")
    private String address3;

    @Size(max = 255, message = "Address line 4 must be at most 255 characters")
    private String address4;

    @Size(max = 30, message = "Phone must be at most 30 characters")
    private String phone;

    @Size(max = 120, message = "Contact person must be at most 120 characters")
    private String contactPerson;

    @Size(max = 20, message = "GSTIN must be at most 20 characters")
    private String gstin;

    private TransportMode transportMode;

    private Map<String, Object> attributes;
}
