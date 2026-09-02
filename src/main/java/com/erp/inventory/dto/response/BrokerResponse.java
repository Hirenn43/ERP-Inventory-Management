package com.erp.inventory.dto.response;

import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrokerResponse {
    private Long id;
    private Long companyId;
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
