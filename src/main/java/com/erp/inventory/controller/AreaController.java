package com.erp.inventory.controller;

import com.erp.inventory.dto.request.AreaRequest;
import com.erp.inventory.dto.response.AreaResponse;
import com.erp.inventory.service.AreaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaService areaService;
    private final com.erp.inventory.config.PatchMerger patchMerger;

    @PostMapping
    public ResponseEntity<AreaResponse> create(@Valid @RequestBody AreaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(areaService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaResponse> update(@PathVariable Long id, @Valid @RequestBody AreaRequest request) {
        return ResponseEntity.ok(areaService.update(id, request));
    }



    @PatchMapping("/{id}")


    public ResponseEntity<AreaResponse> patch(@PathVariable Long id,


            @org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode patch) {


        AreaRequest merged = patchMerger.merge(areaService.getById(id), AreaRequest.class, patch);


        return ResponseEntity.ok(areaService.update(id, merged));


    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(areaService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(areaService.getAll());
        }
        return ResponseEntity.ok(areaService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        areaService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
