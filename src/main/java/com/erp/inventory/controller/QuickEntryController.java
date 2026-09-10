package com.erp.inventory.controller;

import com.erp.inventory.dto.request.QuickEntryParseRequest;
import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.service.QuickEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Natural-language quick entry. {@code /parse} only interprets text into a draft - the frontend
 *  submits it through the normal create endpoints. */
@RestController
@RequestMapping("/quick-entry")
@RequiredArgsConstructor
public class QuickEntryController {

    private final QuickEntryService quickEntryService;

    @PostMapping("/parse")
    public ResponseEntity<QuickEntryResult> parse(@Valid @RequestBody QuickEntryParseRequest request) {
        return ResponseEntity.ok(quickEntryService.parse(request));
    }
}
