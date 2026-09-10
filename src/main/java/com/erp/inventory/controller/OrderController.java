package com.erp.inventory.controller;

import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.request.OrderRequest;
import com.erp.inventory.dto.response.OrderMaterialRequirementResponse;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/** Finished-goods orders. Fulfillment is tracked as a ledger of events (see /fulfillment-events),
 *  not a single status column - an order can be partially completed, dispatched, settled and returned. */
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request,
                                                 @AuthenticationPrincipal UserPrincipal actor) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.create(request, actor));
    }

    /** Preview how much yarn an order of {@code qty} units of a product would use vs current stock. */
    @GetMapping("/material-requirement")
    public ResponseEntity<OrderMaterialRequirementResponse> materialRequirement(
            @RequestParam Long productId,
            @RequestParam(required = false) Long componentId,
            @RequestParam BigDecimal qty) {
        return ResponseEntity.ok(orderService.computeRequirement(productId, componentId, qty));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(orderService.getAll());
        }
        return ResponseEntity.ok(orderService.getPage(pageable, q));
    }

    @PostMapping("/{id}/fulfillment-events")
    public ResponseEntity<OrderResponse> addFulfillmentEvent(@PathVariable Long id,
                                                               @Valid @RequestBody OrderFulfillmentEventRequest request) {
        return ResponseEntity.ok(orderService.addFulfillmentEvent(id, request));
    }
}
