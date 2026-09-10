package com.erp.inventory.controller;

import com.erp.inventory.dto.request.MaterialRequest;
import com.erp.inventory.dto.response.MaterialResponse;
import com.erp.inventory.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @PostMapping
    public ResponseEntity<MaterialResponse> create(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(materialService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<MaterialResponse>> getAll() {
        return ResponseEntity.ok(materialService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
