package com.erp.inventory.controller;

import com.erp.inventory.client.WhatsAppClient;
import com.erp.inventory.config.WhatsAppProperties;
import com.erp.inventory.service.whatsapp.WhatsAppQuickEntryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppWebhookControllerTest {

    @Mock WhatsAppQuickEntryService quickEntryService;
    @Mock WhatsAppClient whatsAppClient;

    WhatsAppProperties properties;
    WhatsAppWebhookController controller;

    private static final String BODY = """
            {"entry":[{"changes":[{"value":{"messages":[
              {"from":"919876543210","type":"text","text":{"body":"new order"}}]}}]}]}""";

    @BeforeEach
    void setUp() {
        properties = new WhatsAppProperties();
        controller = new WhatsAppWebhookController(properties, quickEntryService, whatsAppClient, new ObjectMapper());
    }

    @Test
    void verify_echoesChallengeWhenTokenMatches() {
        properties.setVerifyToken("s3cr3t");
        var response = controller.verify("subscribe", "s3cr3t", "CHALLENGE-42");
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("CHALLENGE-42");
    }

    @Test
    void verify_rejectsWrongToken() {
        properties.setVerifyToken("s3cr3t");
        var response = controller.verify("subscribe", "wrong", "CHALLENGE-42");
        assertThat(response.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void receive_noSecretConfigured_processesMessage() {
        properties.setAppSecret("");
        when(quickEntryService.handleInbound(eq("919876543210"), eq("new order"))).thenReturn("Customer name?");

        var response = controller.receive(BODY.getBytes(StandardCharsets.UTF_8), null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(whatsAppClient).sendText("919876543210", "Customer name?");
    }

    @Test
    void receive_validSignature_processesMessage() throws Exception {
        properties.setAppSecret("appsecret");
        byte[] raw = BODY.getBytes(StandardCharsets.UTF_8);
        when(quickEntryService.handleInbound(any(), any())).thenReturn("ok");

        var response = controller.receive(raw, sign(raw, "appsecret"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(quickEntryService).handleInbound("919876543210", "new order");
    }

    @Test
    void receive_badSignature_rejectedWithoutProcessing() {
        properties.setAppSecret("appsecret");
        byte[] raw = BODY.getBytes(StandardCharsets.UTF_8);

        var response = controller.receive(raw, "sha256=deadbeef");

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        verify(quickEntryService, never()).handleInbound(any(), any());
    }

    @Test
    void receive_garbageBody_stillReturns200() {
        properties.setAppSecret("");
        var response = controller.receive("not json".getBytes(StandardCharsets.UTF_8), null);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    private static String sign(byte[] body, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
}
