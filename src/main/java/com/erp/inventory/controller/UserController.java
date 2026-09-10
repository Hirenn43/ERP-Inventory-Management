package com.erp.inventory.controller;

import com.erp.inventory.dto.request.PasswordResetRequest;
import com.erp.inventory.dto.request.UserCreateRequest;
import com.erp.inventory.dto.request.UserRolesRequest;
import com.erp.inventory.dto.request.UserUpdateRequest;
import com.erp.inventory.dto.response.UserResponse;
import com.erp.inventory.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Support → User Management. Requires {@code ROLE_USER_ADMIN} (or {@code ROLE_ADMIN}). */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','USER_ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(userService.getAll());
        }
        return ResponseEntity.ok(userService.getPage(pageable, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponse> setRoles(@PathVariable Long id,
                                                 @Valid @RequestBody UserRolesRequest request) {
        return ResponseEntity.ok(userService.setRoles(id, request));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id,
                                              @Valid @RequestBody PasswordResetRequest request) {
        userService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/enabled")
    public ResponseEntity<UserResponse> setEnabled(@PathVariable Long id, @RequestParam boolean value) {
        return ResponseEntity.ok(userService.setEnabled(id, value));
    }
}
