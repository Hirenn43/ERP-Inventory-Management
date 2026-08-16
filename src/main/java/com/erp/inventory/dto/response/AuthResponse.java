package com.erp.inventory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Set;

@Getter
@Builder
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long userId;
    private String username;
    private Set<String> roles;
}
