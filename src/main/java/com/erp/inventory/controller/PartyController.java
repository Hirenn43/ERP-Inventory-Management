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

    @PostMapping
    public ResponseEntity<PartyResponse> create(@Valid @RequestBody PartyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(partyService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PartyResponse> update(@PathVariable Long id, @Valid @RequestBody PartyRequest request) {
        return ResponseEntity.ok(partyService.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PartyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(partyService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PartyResponse>> getAll() {
        return ResponseEntity.ok(partyService.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        partyService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
