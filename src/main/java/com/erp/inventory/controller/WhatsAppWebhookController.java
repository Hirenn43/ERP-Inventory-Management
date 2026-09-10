package com.erp.inventory.controller;

import com.erp.inventory.config.WhatsAppProperties;
import com.erp.inventory.dto.whatsapp.WaWebhookPayload;
import com.erp.inventory.service.whatsapp.WebhookIdempotencyService;
import com.erp.inventory.service.whatsapp.WhatsAppQuickEntryService;
import com.erp.inventory.client.WhatsAppClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Meta WhatsApp Cloud API webhook. Public (see SecurityConfig) - POST is protected by the
 *  {@code X-Hub-Signature-256} HMAC instead of a JWT. */
@RestController
@RequestMapping("/whatsapp/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final WhatsAppProperties properties;
    private final WhatsAppQuickEntryService quickEntryService;
    private final WhatsAppClient whatsAppClient;
    private final ObjectMapper objectMapper;
    private final WebhookIdempotencyService idempotencyService;

    /** Verification handshake Meta calls once when you register the callback URL. */
    @GetMapping
    public ResponseEntity<String> verify(@RequestParam(name = "hub.mode", required = false) String mode,
                                         @RequestParam(name = "hub.verify_token", required = false) String token,
                                         @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if ("subscribe".equals(mode) && properties.getVerifyToken().equals(token)) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(403).body("Verification failed");
    }

    /** Inbound messages. Always answers 200 so Meta does not retry; failures are logged. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(@RequestBody byte[] rawBody,
                                        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        try {
            if (!signatureValid(rawBody, signature)) {
                log.warn("WhatsApp webhook: bad or missing signature");
                return ResponseEntity.status(403).build();
            }
            WaWebhookPayload payload = objectMapper.readValue(rawBody, WaWebhookPayload.class);
            for (WaWebhookPayload.TextMessage msg : payload.textMessages()) {
                // Claim the id up-front and durably (own transaction). A retry or a concurrent
                // redelivery that lost the race is skipped here, before any side effect.
                boolean claimed = msg.id() != null && idempotencyService.tryClaim(msg.id());
                if (msg.id() != null && !claimed) {
                    log.info("WhatsApp webhook: skipping already-processed message {}", msg.id());
                    continue;
                }
                try {
                    String reply = quickEntryService.handleInbound(msg.from(), msg.body());
                    whatsAppClient.sendText(msg.from(), reply);
                } catch (RuntimeException ex) {
                    // One bad message must not block the rest of the batch. Release the claim so
                    // Meta's redelivery retries just this message.
                    log.error("WhatsApp webhook: message {} from {} failed", msg.id(), msg.from(), ex);
                    if (claimed) {
                        idempotencyService.release(msg.id());
                    }
                }
            }
        } catch (Exception e) {
            log.error("WhatsApp webhook processing error", e);
        }
        return ResponseEntity.ok().build();
    }

    private boolean signatureValid(byte[] body, String header) {
        String secret = properties.getAppSecret();
        if (secret == null || secret.isBlank()) {
            return true; // local/dev: no secret configured -> skip the check
        }
        if (header == null || !header.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    header.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("Signature check failed", e);
            return false;
        }
    }
}
