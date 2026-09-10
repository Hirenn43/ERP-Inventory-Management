package com.erp.inventory.controller;

import com.erp.inventory.dto.request.MaterialRequest;
import com.erp.inventory.dto.response.MaterialResponse;
import com.erp.inventory.service.MaterialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;
    private final com.erp.inventory.config.PatchMerger patchMerger;

    @PostMapping
    public ResponseEntity<MaterialResponse> create(@Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(materialService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaterialResponse> update(@PathVariable Long id, @Valid @RequestBody MaterialRequest request) {
        return ResponseEntity.ok(materialService.update(id, request));
    }



    @PatchMapping("/{id}")


    public ResponseEntity<MaterialResponse> patch(@PathVariable Long id,


            @org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode patch) {


        MaterialRequest merged = patchMerger.merge(materialService.getById(id), MaterialRequest.class, patch);


        return ResponseEntity.ok(materialService.update(id, merged));


    }

    @GetMapping("/{id}")
    public ResponseEntity<MaterialResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(materialService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(materialService.getAll());
        }
        return ResponseEntity.ok(materialService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
