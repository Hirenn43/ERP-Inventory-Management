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
@RequestMapping("/transporters")
@RequiredArgsConstructor
public class TransporterController {

    private final TransporterService transporterService;
    private final com.erp.inventory.config.PatchMerger patchMerger;

    @PostMapping
    public ResponseEntity<TransporterResponse> create(@Valid @RequestBody TransporterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(transporterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransporterResponse> update(@PathVariable Long id, @Valid @RequestBody TransporterRequest request) {
        return ResponseEntity.ok(transporterService.update(id, request));
    }



    @PatchMapping("/{id}")


    public ResponseEntity<TransporterResponse> patch(@PathVariable Long id,


            @org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode patch) {


        TransporterRequest merged = patchMerger.merge(transporterService.getById(id), TransporterRequest.class, patch);


        return ResponseEntity.ok(transporterService.update(id, merged));


    }

    @GetMapping("/{id}")
    public ResponseEntity<TransporterResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(transporterService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(transporterService.getAll());
        }
        return ResponseEntity.ok(transporterService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        transporterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
