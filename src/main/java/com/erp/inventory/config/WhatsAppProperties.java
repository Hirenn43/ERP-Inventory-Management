package com.erp.inventory.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Binds {@code app.whatsapp.*}. See application.yml for the env-var overrides. */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    /** When false the outbound Meta call is skipped (the conversation still runs, replies are logged). */
    private boolean enabled = false;

    private String apiBase = "https://graph.facebook.com/v21.0";
    private String phoneNumberId = "";
    private String accessToken = "";

    /** Echoed back during the GET webhook verification handshake. */
    private String verifyToken = "changeme";

    /** App secret for the {@code X-Hub-Signature-256} check. Blank -> signature check is skipped. */
    private String appSecret = "";

    /** Idle expiry for a pending conversation held in Redis. */
    private long conversationTtlSeconds = 900;

    public boolean hasCredentials() {
        return phoneNumberId != null && !phoneNumberId.isBlank()
                && accessToken != null && !accessToken.isBlank();
    }
}
