package com.erp.inventory.controller;

import com.erp.inventory.dto.request.PurchaseOrderRequest;
import com.erp.inventory.dto.response.PurchaseOrderResponse;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @PostMapping
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request,
                                                          @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(purchaseOrderService.create(request, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PurchaseOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PurchaseOrderResponse>> getAll() {
        return ResponseEntity.ok(purchaseOrderService.getAll());
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<PurchaseOrderResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.complete(id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PurchaseOrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.cancel(id));
    }
}
