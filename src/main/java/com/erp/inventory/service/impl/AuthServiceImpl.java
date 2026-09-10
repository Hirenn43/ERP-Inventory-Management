package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.LoginRequest;
import com.erp.inventory.dto.request.RefreshTokenRequest;
import com.erp.inventory.dto.request.RegisterRequest;
import com.erp.inventory.dto.response.AuthResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Role;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.enums.RoleName;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.TokenRefreshException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.RoleRepository;
import com.erp.inventory.repository.UserRepository;
import com.erp.inventory.security.JwtService;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Single generic message for both cases to avoid username / email enumeration.
        if (userRepository.existsByUsername(request.getUsername())
                || userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Username or email is already in use");
        }

        // Self-service registration always creates a plain staff account.
        // Elevated roles must be granted by an administrator, never requested by the caller.
        Set<Role> roles = new HashSet<>();
        roles.add(getOrCreateRole(RoleName.ROLE_STAFF));

        // Every user belongs to a tenant. A new sign-up starts its own company.
        String companyName = request.getCompanyName() != null && !request.getCompanyName().isBlank()
                ? request.getCompanyName().trim()
                : request.getFullName();
        Company company = companyRepository.save(Company.builder().name(companyName).build());

        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
                .company(company)
                .enabled(true)
                .build();

        userRepository.save(user);

        UserPrincipal principal = new UserPrincipal(user);
        return buildAuthResponse(principal);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new BadCredentialsException("Invalid username or password");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return buildAuthResponse(principal);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new TokenRefreshException("Provided token is not a refresh token");
        }

        String username = jwtService.extractUsername(refreshToken);
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(refreshToken, principal)) {
            throw new TokenRefreshException("Refresh token is invalid or expired");
        }

        return buildAuthResponse(principal);
    }

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

    private AuthResponse buildAuthResponse(UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String refreshToken = jwtService.generateRefreshToken(principal);

        Set<String> roleNames = principal.getAuthorities().stream()
                .map(Object::toString)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(principal.getId())
                .username(principal.getUsername())
                .roles(roleNames)
                .build();
    }
}
