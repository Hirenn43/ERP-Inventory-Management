package com.erp.inventory.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** Just enough of the Meta WhatsApp webhook body to pull the inbound text messages.
 *  Status callbacks and other fields are ignored. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaWebhookPayload {

    private List<Entry> entry;

    /** Every inbound text message in the payload, in order. Meta batches multiple messages
     *  into one POST, so all of them must be processed - not just the first. */
    public List<TextMessage> textMessages() {
        if (entry == null) {
            return List.of();
        }
        return entry.stream()
                .filter(e -> e.changes != null)
                .flatMap(e -> e.changes.stream())
                .map(c -> c.value)
                .filter(v -> v != null && v.messages != null)
                .flatMap(v -> v.messages.stream())
                .filter(m -> "text".equals(m.type) && m.text != null && m.text.body != null)
                .map(m -> new TextMessage(m.id, m.from, m.text.body))
                .toList();
    }

    /** Meta's message id, used for webhook idempotency. May be null on malformed payloads. */
    public record TextMessage(String id, String from, String body) {
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        private List<Change> changes;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Change {
        private Value value;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Value {
        private List<Message> messages;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String id;
        private String from;
        private String type;
        private Text text;
    }

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Text {
        private String body;
    }
}
