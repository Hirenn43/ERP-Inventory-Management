package com.erp.inventory.controller;

import com.erp.inventory.dto.request.SalesOrderRequest;
import com.erp.inventory.dto.response.SalesOrderResponse;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @PostMapping
    public ResponseEntity<SalesOrderResponse> create(@Valid @RequestBody SalesOrderRequest request,
                                                       @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(salesOrderService.create(request, actor));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SalesOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(salesOrderService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<SalesOrderResponse>> getAll() {
        return ResponseEntity.ok(salesOrderService.getAll());
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<SalesOrderResponse> cancel(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.ok(salesOrderService.cancel(id, actor));
    }
}
