package com.erp.inventory.service;

import com.erp.inventory.dto.request.LoginRequest;
import com.erp.inventory.dto.request.RefreshTokenRequest;
import com.erp.inventory.dto.request.RegisterRequest;
import com.erp.inventory.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(RefreshTokenRequest request);
}
