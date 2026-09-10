package com.erp.inventory.controller;

import com.erp.inventory.service.whatsapp.WhatsAppQuickEntryService;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Dev-only: drive the WhatsApp quick-entry flow without Meta. POST a phone + message, get the
 *  bot's reply. Registered only under the {@code dev} profile. Public (see SecurityConfig). */
@RestController
@RequestMapping("/whatsapp/simulate")
@RequiredArgsConstructor
@Profile("dev")
public class WhatsAppSimulatorController {

    private final WhatsAppQuickEntryService quickEntryService;

    @PostMapping
    public ResponseEntity<Map<String, String>> simulate(@RequestBody SimulateRequest request) {
        String reply = quickEntryService.handleInbound(request.getPhone(), request.getText());
        return ResponseEntity.ok(Map.of("reply", reply));
    }

    @Getter
    @Setter
    public static class SimulateRequest {
        @NotBlank
        private String phone;
        @NotBlank
        private String text;
    }
}
