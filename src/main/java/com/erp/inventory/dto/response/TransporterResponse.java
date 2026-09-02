package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.TransportMode;
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
public class TransporterResponse {
    private Long id;
    private Long companyId;
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
