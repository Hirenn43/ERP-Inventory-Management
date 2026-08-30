package com.erp.inventory.service.whatsapp;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Test double for {@link WaConversationStore} - no Redis, no TTL. */
public class InMemoryWaConversationStore implements WaConversationStore {

    private final Map<String, WaConversation> map = new HashMap<>();

    @Override
    public Optional<WaConversation> find(String phone) {
        return Optional.ofNullable(map.get(phone));
    }

    @Override
    public void save(WaConversation conversation) {
        map.put(conversation.getPhone(), conversation);
    }

    @Override
    public void delete(String phone) {
        map.remove(phone);
    }

    public boolean has(String phone) {
        return map.containsKey(phone);
    }
}
