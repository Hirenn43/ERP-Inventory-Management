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
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final com.erp.inventory.config.PatchMerger patchMerger;

    @PostMapping
    public ResponseEntity<WarehouseResponse> create(@Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WarehouseResponse> update(@PathVariable Long id, @Valid @RequestBody WarehouseRequest request) {
        return ResponseEntity.ok(warehouseService.update(id, request));
    }



    @PatchMapping("/{id}")


    public ResponseEntity<WarehouseResponse> patch(@PathVariable Long id,


            @org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode patch) {


        WarehouseRequest merged = patchMerger.merge(warehouseService.getById(id), WarehouseRequest.class, patch);


        return ResponseEntity.ok(warehouseService.update(id, merged));


    }

    @GetMapping("/{id}")
    public ResponseEntity<WarehouseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(warehouseService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(warehouseService.getAll());
        }
        return ResponseEntity.ok(warehouseService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
