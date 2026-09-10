package com.erp.inventory.client;

import com.erp.inventory.config.WhatsAppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.Map;

/** Outbound WhatsApp text sender via the Meta Graph API. No-ops (logs only) when the integration
 *  is disabled or credentials are missing, so the conversation logic can run in local/dev without
 *  any Meta setup. */
@Component
public class WhatsAppClient {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppClient.class);

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(8);

    private final WhatsAppProperties properties;
    private final RestClient client;

    public WhatsAppClient(WhatsAppProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        // One reusable client with explicit timeouts - a hung TCP connection to graph.facebook.com
        // must never pin a Tomcat worker indefinitely.
        this.client = restClientBuilder
                .baseUrl(properties.getApiBase())
                .requestFactory(ClientHttpRequestFactories.get(
                        ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(CONNECT_TIMEOUT)
                                .withReadTimeout(READ_TIMEOUT)))
                .build();
    }

    public void sendText(String toPhone, String body) {
        if (!properties.isEnabled() || !properties.hasCredentials()) {
            log.info("[whatsapp disabled] would reply to {}: {}", toPhone, body);
            return;
        }
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
