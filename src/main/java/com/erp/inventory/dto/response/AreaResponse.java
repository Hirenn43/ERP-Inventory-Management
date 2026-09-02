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
public class AreaResponse {
    private Long id;
    private Long companyId;
    private String name;
    private String contactMobile;
    private BigDecimal rate;
    private Map<String, Object> attributes;
}
