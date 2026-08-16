package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.LoginRequest;
import com.erp.inventory.dto.request.RefreshTokenRequest;
import com.erp.inventory.dto.request.RegisterRequest;
import com.erp.inventory.dto.response.AuthResponse;
import com.erp.inventory.entity.Role;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.enums.RoleName;
import com.erp.inventory.exception.DuplicateResourceException;
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
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered: " + request.getEmail());
        }

        Set<Role> roles = resolveRoles(request.getRoles());

        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(roles)
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
        String username = jwtService.extractUsername(request.getRefreshToken());
        UserPrincipal principal = (UserPrincipal) userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(request.getRefreshToken(), principal)) {
            throw new BadCredentialsException("Refresh token is invalid or expired");
        }

        return buildAuthResponse(principal);
    }

    private Set<Role> resolveRoles(Set<String> requestedRoles) {
        Set<Role> roles = new HashSet<>();
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            roles.add(getOrCreateRole(RoleName.ROLE_STAFF));
            return roles;
        }
        for (String roleName : requestedRoles) {
            RoleName resolved = RoleName.valueOf(roleName.trim().toUpperCase());
            roles.add(getOrCreateRole(resolved));
        }
        return roles;
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
