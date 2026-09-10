package com.erp.inventory.controller;

import com.erp.inventory.dto.request.StockAdjustmentRequest;
import com.erp.inventory.dto.response.InventoryResponse;
import com.erp.inventory.dto.response.StockMovementResponse;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<List<InventoryResponse>> getAll() {
        return ResponseEntity.ok(inventoryService.getAll());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<InventoryResponse>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getByProduct(productId));
    }

    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<List<InventoryResponse>> getByWarehouse(@PathVariable Long warehouseId) {
        return ResponseEntity.ok(inventoryService.getByWarehouse(warehouseId));
    }

    @PostMapping("/adjust")
    public ResponseEntity<StockMovementResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request,
                                                               @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(inventoryService.adjustStock(request, actor));
    }

    @GetMapping("/movements/product/{productId}")
    public ResponseEntity<List<StockMovementResponse>> getMovements(@PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getMovementsForProduct(productId));
    }
}
