package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.PasswordResetRequest;
import com.erp.inventory.dto.request.UserCreateRequest;
import com.erp.inventory.dto.request.UserRolesRequest;
import com.erp.inventory.dto.request.UserUpdateRequest;
import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.dto.response.UserResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Role;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.enums.RoleName;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.RoleRepository;
import com.erp.inventory.repository.UserRepository;
import com.erp.inventory.repository.spec.TextSearchSpec;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.UserService;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<UserResponse> getAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<UserResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(
                userRepository.findAll(TextSearchSpec.contains(q, "username", "fullName", "email"), pageable)
                        .map(this::toResponse));
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())
                || userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Username or email is already in use");
        }
        User user = User.builder()
                .fullName(request.getFullName().trim())
                .username(request.getUsername().trim())
                .email(request.getEmail().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .roles(resolveRoles(request.getRoles()))
                .company(currentCompany())
                .enabled(true)
                .build();
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = findEntity(id);
        user.setFullName(request.getFullName().trim());
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use");
        }
        user.setEmail(request.getEmail().trim());
        if (request.getEnabled() != null) {
            if (!request.getEnabled()) {
                guardLastUserAdmin(user);
            }
            user.setEnabled(request.getEnabled());
        }
        return toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse setRoles(Long id, UserRolesRequest request) {
        User user = findEntity(id);
        Set<Role> newRoles = resolveRoles(request.getRoles());
        boolean losesUserAdmin = user.getRoles().stream().anyMatch(r -> r.getName() == RoleName.ROLE_USER_ADMIN)
                && newRoles.stream().noneMatch(r -> r.getName() == RoleName.ROLE_USER_ADMIN);
        if (losesUserAdmin) {
            guardLastUserAdmin(user);
        }
        user.setRoles(newRoles);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, PasswordResetRequest request) {
        User user = findEntity(id);
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    @Transactional
    public UserResponse setEnabled(Long id, boolean enabled) {
        User user = findEntity(id);
        if (!enabled) {
            guardLastUserAdmin(user);
        }
        user.setEnabled(enabled);
        return toResponse(user);
    }

    // --- helpers -------------------------------------------------------

    /** Resolve role names to entities; a user-admin may never mint a full ROLE_ADMIN. */
    private Set<Role> resolveRoles(Set<RoleName> names) {
        if (names.contains(RoleName.ROLE_ADMIN)) {
            throw new AccessDeniedException("ROLE_ADMIN can only be granted by a system administrator");
        }
        Set<Role> roles = new HashSet<>();
        for (RoleName rn : names) {
            roles.add(roleRepository.findByName(rn)
                    .orElseThrow(() -> ResourceNotFoundException.of("Role", rn)));
        }
        return roles;
    }

    /** Refuse an action that would leave the tenant with no enabled user-admin (or admin). */
    private void guardLastUserAdmin(User user) {
        boolean isUserAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ROLE_USER_ADMIN || r.getName() == RoleName.ROLE_ADMIN);
        if (!isUserAdmin) {
            return;
        }
        Long companyId = TenantContext.get();
        long admins = userRepository.countByRoles_NameAndCompany_IdAndEnabledTrue(RoleName.ROLE_USER_ADMIN, companyId)
                + userRepository.countByRoles_NameAndCompany_IdAndEnabledTrue(RoleName.ROLE_ADMIN, companyId);
        if (admins <= 1) {
            throw new IllegalArgumentException(
                    "The tenant must keep at least one active user administrator");
        }
    }

    private Company currentCompany() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return companyRepository.getReferenceById(tenantId);
    }

    private User findEntity(Long id) {
        return userRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .fullName(u.getFullName())
                .username(u.getUsername())
                .email(u.getEmail())
                .enabled(u.isEnabled())
                .roles(u.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .createdAt(u.getCreatedAt())
                .build();
    }
}
