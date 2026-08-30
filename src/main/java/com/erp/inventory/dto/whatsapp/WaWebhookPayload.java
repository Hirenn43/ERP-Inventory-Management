package com.erp.inventory.dto.whatsapp;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

/** Just enough of the Meta WhatsApp webhook body to pull the first inbound text message.
 *  Status callbacks and other fields are ignored. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WaWebhookPayload {

    private List<Entry> entry;

    /** First inbound text message in the payload, if any. */
    public Optional<TextMessage> firstTextMessage() {
        if (entry == null) {
            return Optional.empty();
        }
        return entry.stream()
                .filter(e -> e.changes != null)
                .flatMap(e -> e.changes.stream())
                .map(c -> c.value)
                .filter(v -> v != null && v.messages != null)
                .flatMap(v -> v.messages.stream())
                .filter(m -> "text".equals(m.type) && m.text != null && m.text.body != null)
                .map(m -> new TextMessage(m.from, m.text.body))
                .findFirst();
    }

    public record TextMessage(String from, String body) {
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
