package com.erp.inventory.controller;

import com.erp.inventory.dto.request.MaterialStockMovementRequest;
import com.erp.inventory.dto.response.MaterialStockMovementResponse;
import com.erp.inventory.service.MaterialStockMovementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Raw-material purchase/sale ledger - direction IN (purchase) or OUT (sale). */
@RestController
@RequestMapping("/material-stock-movements")
@RequiredArgsConstructor
public class MaterialStockMovementController {

    private final MaterialStockMovementService movementService;

    @PostMapping
    public ResponseEntity<MaterialStockMovementResponse> create(@Valid @RequestBody MaterialStockMovementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movementService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<MaterialStockMovementResponse>> getAll() {
        return ResponseEntity.ok(movementService.getAll());
    }

    @GetMapping("/material/{materialId}")
    public ResponseEntity<List<MaterialStockMovementResponse>> getByMaterial(@PathVariable Long materialId) {
        return ResponseEntity.ok(movementService.getByMaterial(materialId));
    }
}
