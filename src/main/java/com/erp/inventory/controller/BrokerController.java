package com.erp.inventory.controller;

import com.erp.inventory.dto.request.BrokerRequest;
import com.erp.inventory.dto.response.BrokerResponse;
import com.erp.inventory.service.BrokerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/brokers")
@RequiredArgsConstructor
public class BrokerController {

    private final BrokerService brokerService;

    @PostMapping
    public ResponseEntity<BrokerResponse> create(@Valid @RequestBody BrokerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(brokerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<BrokerResponse> update(@PathVariable Long id, @Valid @RequestBody BrokerRequest request) {
        return ResponseEntity.ok(brokerService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BrokerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(brokerService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(brokerService.getAll());
        }
        return ResponseEntity.ok(brokerService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brokerService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
