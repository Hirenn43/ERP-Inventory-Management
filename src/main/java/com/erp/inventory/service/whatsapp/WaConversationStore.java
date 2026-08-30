package com.erp.inventory.service.whatsapp;

import java.util.Optional;

/** Persistence for in-flight WhatsApp conversations. Redis-backed in production; an in-memory
 *  fake is used in unit tests. */
public interface WaConversationStore {

    Optional<WaConversation> find(String phone);

    /** Stores the conversation and (re)sets its idle TTL. */
    void save(WaConversation conversation);

    void delete(String phone);
}
