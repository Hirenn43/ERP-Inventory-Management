package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhatsappUserLinkResponse {
    private Long id;
    private String phone;
    private Long userId;
    private String username;
    private Long companyId;
    private String companyName;
    private boolean enabled;
    private LocalDateTime createdAt;
}
