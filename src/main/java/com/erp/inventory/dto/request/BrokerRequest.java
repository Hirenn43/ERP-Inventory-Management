package com.erp.inventory.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BrokerRequest {

    private Long companyId;

    @NotBlank(message = "Broker name is required")
    private String name;

    private String address1;

    private String address2;

    private String address3;

    private String address4;

    private String phone1;

    private String contactPerson1;

    private String phone2;

    private String contactPerson2;

    private String email;

    private BigDecimal brokeragePct;

    private Boolean blacklisted;

    private Map<String, Object> attributes;
}
