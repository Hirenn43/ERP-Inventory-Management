package com.erp.inventory.client;

import com.erp.inventory.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/** Outbound WhatsApp text sender via the Meta Graph API. No-ops (logs only) when the integration
 *  is disabled or credentials are missing, so the conversation logic can run in local/dev without
 *  any Meta setup. */
@Component
@RequiredArgsConstructor
public class WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClient.class);

    private final WhatsAppProperties properties;
    private final RestClient.Builder restClientBuilder;

    public void sendText(String toPhone, String body) {
        if (!properties.isEnabled() || !properties.hasCredentials()) {
            log.info("[whatsapp disabled] would reply to {}: {}", toPhone, body);
            return;
        }
        RestClient client = restClientBuilder.baseUrl(properties.getApiBase()).build();
        Map<String, Object> payload = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", toPhone,
                "type", "text",
                "text", Map.of("preview_url", false, "body", body));
        try {
            client.post()
                    .uri("/{phoneNumberId}/messages", properties.getPhoneNumberId())
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("WhatsApp send to {} failed: {}", toPhone, e.getMessage());
        }
    }
}
