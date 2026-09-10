package com.erp.inventory.controller;

import com.erp.inventory.dto.request.ProductComponentRequest;
import com.erp.inventory.dto.response.ProductComponentResponse;
import com.erp.inventory.service.ProductComponentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Bill-of-materials lines: which Material fills which slot of a Product. */
@RestController
@RequestMapping("/product-components")
@RequiredArgsConstructor
public class ProductComponentController {

    private final ProductComponentService productComponentService;

    @PostMapping
    public ResponseEntity<ProductComponentResponse> create(@Valid @RequestBody ProductComponentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productComponentService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductComponentResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody ProductComponentRequest request) {
        return ResponseEntity.ok(productComponentService.update(id, request));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<List<ProductComponentResponse>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productComponentService.getByProduct(productId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productComponentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
