package com.erp.inventory.service.whatsapp;

import com.erp.inventory.entity.enums.QuickEntryAction;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One in-flight WhatsApp quick-entry conversation, held in Redis keyed by the sender's phone. */
@Getter
@Setter
@NoArgsConstructor
public class WaConversation {

    private String phone;
    private QuickEntryAction action = QuickEntryAction.UNKNOWN;

    /** Accumulated payload, already shaped like the target *Request DTO. */
    private Map<String, Object> fields = new LinkedHashMap<>();

    /** The required field we last asked the user to supply. */
    private String currentField;

    /** When set, the user was shown a numbered pick list for {@code menuField}. */
    private String menuField;
    private List<Long> menuIds = new ArrayList<>();

    /** Consecutive unparseable replies for {@code currentField}; conversation is dropped after 3. */
    private int fieldAttempts;

    private Instant createdAt = Instant.now();
    private Instant lastActivityAt = Instant.now();

    public void clearMenu() {
        this.menuField = null;
        this.menuIds = new ArrayList<>();
    }
}
