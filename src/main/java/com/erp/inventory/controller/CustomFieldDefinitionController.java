package com.erp.inventory.controller;

import com.erp.inventory.config.PatchMerger;
import com.erp.inventory.dto.request.CustomFieldDefinitionRequest;
import com.erp.inventory.dto.response.CustomFieldDefinitionResponse;
import com.erp.inventory.service.CustomFieldDefinitionService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Master Management — the per-tenant custom-field definitions.
 * Reads are open to any authenticated user (the Details forms need them);
 * writes require {@code ROLE_MASTER_ADMIN} (or {@code ROLE_ADMIN}).
 */
@RestController
@RequestMapping("/custom-fields")
@RequiredArgsConstructor
public class CustomFieldDefinitionController {

    private final CustomFieldDefinitionService service;
    private final PatchMerger patchMerger;

    @GetMapping
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) String resource,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer page,
            Pageable pageable) {
        if (resource != null && !resource.isBlank()) {
            return ResponseEntity.ok(service.getByResource(resource, includeInactive));
        }
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(service.getAll());
        }
        return ResponseEntity.ok(service.getPage(pageable, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomFieldDefinitionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MASTER_ADMIN')")
    public ResponseEntity<CustomFieldDefinitionResponse> create(
            @Valid @RequestBody CustomFieldDefinitionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PostMapping("/seed-defaults")
    @PreAuthorize("hasAnyRole('ADMIN','MASTER_ADMIN')")
    public ResponseEntity<List<CustomFieldDefinitionResponse>> seedDefaults(
            @RequestParam String resource,
            @Valid @RequestBody List<CustomFieldDefinitionRequest> defs) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.seedDefaults(resource, defs));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MASTER_ADMIN')")
    public ResponseEntity<CustomFieldDefinitionResponse> update(
            @PathVariable Long id, @Valid @RequestBody CustomFieldDefinitionRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MASTER_ADMIN')")
    public ResponseEntity<CustomFieldDefinitionResponse> patch(
            @PathVariable Long id, @RequestBody JsonNode patch) {
        CustomFieldDefinitionRequest merged =
                patchMerger.merge(service.getById(id), CustomFieldDefinitionRequest.class, patch);
        return ResponseEntity.ok(service.update(id, merged));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MASTER_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
