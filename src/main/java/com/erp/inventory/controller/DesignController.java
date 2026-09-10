package com.erp.inventory.controller;

import com.erp.inventory.dto.request.DesignRequest;
import com.erp.inventory.dto.response.DesignResponse;
import com.erp.inventory.service.DesignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/designs")
@RequiredArgsConstructor
public class DesignController {

    private final DesignService designService;
    private final com.erp.inventory.config.PatchMerger patchMerger;

    @PostMapping
    public ResponseEntity<DesignResponse> create(@Valid @RequestBody DesignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(designService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DesignResponse> update(@PathVariable Long id, @Valid @RequestBody DesignRequest request) {
        return ResponseEntity.ok(designService.update(id, request));
    }



    @PatchMapping("/{id}")


    public ResponseEntity<DesignResponse> patch(@PathVariable Long id,


            @org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode patch) {


        DesignRequest merged = patchMerger.merge(designService.getById(id), DesignRequest.class, patch);


        return ResponseEntity.ok(designService.update(id, merged));


    }

    @GetMapping("/{id}")
    public ResponseEntity<DesignResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(designService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(designService.getAll());
        }
        return ResponseEntity.ok(designService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        designService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
