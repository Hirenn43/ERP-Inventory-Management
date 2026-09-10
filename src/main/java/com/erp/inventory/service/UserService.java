package com.erp.inventory.service;

import com.erp.inventory.dto.request.PasswordResetRequest;
import com.erp.inventory.dto.request.UserCreateRequest;
import com.erp.inventory.dto.request.UserRolesRequest;
import com.erp.inventory.dto.request.UserUpdateRequest;
import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.dto.response.UserResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

/** Tenant-scoped user administration (Support → User Management). */
public interface UserService {

    List<UserResponse> getAll();

    PageResponse<UserResponse> getPage(Pageable pageable, String q);

    UserResponse getById(Long id);

    UserResponse create(UserCreateRequest request);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse setRoles(Long id, UserRolesRequest request);

    void resetPassword(Long id, PasswordResetRequest request);

    UserResponse setEnabled(Long id, boolean enabled);
}
