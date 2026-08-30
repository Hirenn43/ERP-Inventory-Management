package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.PartyRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private Map<String, Object> attributes;
}
