package com.erp.inventory.controller;

import com.erp.inventory.dto.request.WhatsappUserLinkRequest;
import com.erp.inventory.dto.response.WhatsappUserLinkResponse;
import com.erp.inventory.service.WhatsappUserLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Admin CRUD for phone -> user/company mappings used by the WhatsApp quick-entry bot.
 *  JWT-protected (not in SecurityConfig's public list). */
@RestController
@RequestMapping("/whatsapp/links")
@RequiredArgsConstructor
public class WhatsappUserLinkController {

    private final WhatsappUserLinkService service;

    @PostMapping
    public ResponseEntity<WhatsappUserLinkResponse> create(@Valid @RequestBody WhatsappUserLinkRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WhatsappUserLinkResponse> update(@PathVariable Long id,
                                                           @Valid @RequestBody WhatsappUserLinkRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WhatsappUserLinkResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<WhatsappUserLinkResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
