package com.erp.inventory.controller;

import com.erp.inventory.dto.request.WarehouseRequest;
import com.erp.inventory.dto.response.WarehouseResponse;
import com.erp.inventory.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @PostMapping
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> update(@PathVariable Long id, @Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<WarehouseResponse>> getAll() {
        return ResponseEntity.ok(warehouseService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
