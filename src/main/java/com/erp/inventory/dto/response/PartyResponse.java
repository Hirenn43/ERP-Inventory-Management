package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.PartyRole;
import com.erp.inventory.entity.enums.RegistrationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartyResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String address;
    private String mobile;
    private String email;
    private Set<PartyRole> roles;
    private String gstin;
    private String pan;
    private String stateCode;
    private RegistrationType registrationType;
    private String city;
    private String state;
    private String pinCode;
    private String contactPersonName;
    private BigDecimal openingBalance;
    private BigDecimal creditLimitAmount;
    private Integer creditLimitDays;
    private Boolean blacklisted;
    private Boolean closed;
    private Long scheduleId;
    private Long brokerId;
    private Long areaId;
    private Map<String, Object> attributes;
}
