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
public class VehicleResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String ownerName;
    private String mobileNo;
    private String contactPerson;
    private String contactMobile;
    private String description;
    private BigDecimal tdsPct;
    private BigDecimal loadingCharge;
    private Boolean closed;
    private Map<String, Object> attributes;
}
