package com.erp.inventory.controller;

import com.erp.inventory.dto.request.TransporterRequest;
import com.erp.inventory.dto.response.TransporterResponse;
import com.erp.inventory.service.TransporterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transporters")
@RequiredArgsConstructor
public class TransporterController {

    private final TransporterService transporterService;

    @PostMapping
    public ResponseEntity<TransporterResponse> create(@Valid @RequestBody TransporterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transporterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransporterResponse> update(@PathVariable Long id, @Valid @RequestBody TransporterRequest request) {
        return ResponseEntity.ok(transporterService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransporterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transporterService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<TransporterResponse>> getAll() {
        return ResponseEntity.ok(transporterService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transporterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
