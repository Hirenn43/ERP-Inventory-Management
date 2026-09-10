package com.erp.inventory.controller;

import com.erp.inventory.dto.request.PartyRequest;
import com.erp.inventory.dto.response.PartyResponse;
import com.erp.inventory.service.PartyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parties")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;
    private final com.erp.inventory.config.PatchMerger patchMerger;

    @PostMapping
    public ResponseEntity<PartyResponse> create(@Valid @RequestBody PartyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartyResponse> update(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
        return ResponseEntity.ok(partyService.update(id, request));
    }



    @PatchMapping("/{id}")


    public ResponseEntity<PartyResponse> patch(@PathVariable Long id,


            @org.springframework.web.bind.annotation.RequestBody com.fasterxml.jackson.databind.JsonNode patch) {


        PartyRequest merged = patchMerger.merge(partyService.getById(id), PartyRequest.class, patch);


        return ResponseEntity.ok(partyService.update(id, merged));


    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.getById(id));
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Integer page,
            org.springframework.data.domain.Pageable pageable) {
        if (page == null && (q == null || q.isBlank())) {
            return ResponseEntity.ok(partyService.getAll());
        }
        return ResponseEntity.ok(partyService.getPage(pageable, q));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
