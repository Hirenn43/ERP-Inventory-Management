package com.erp.inventory.controller;

import com.erp.inventory.dto.request.DesignRequest;
import com.erp.inventory.dto.response.DesignResponse;
import com.erp.inventory.service.DesignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;

    @PostMapping
    public ResponseEntity<DesignResponse> create(@Valid @RequestBody DesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> update(@PathVariable Long id, @Valid @RequestBody DesignRequest request) {
        return ResponseEntity.ok(designService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(designService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<DesignResponse>> getAll() {
        return ResponseEntity.ok(designService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        designService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
