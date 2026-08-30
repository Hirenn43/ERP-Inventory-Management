package com.erp.inventory.service.whatsapp;

import com.erp.inventory.config.WhatsAppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RedisWaConversationStore implements WaConversationStore {

    private static final String KEY_PREFIX = "wa:conv:";

    private final RedisTemplate<String, WaConversation> redis;
    private final WhatsAppProperties properties;

    @Override
    public Optional<WaConversation> find(String phone) {
        return Optional.ofNullable(redis.opsForValue().get(key(phone)));
    }

    @Override
    public void save(WaConversation conversation) {
        redis.opsForValue().set(
                key(conversation.getPhone()),
                conversation,
                Duration.ofSeconds(properties.getConversationTtlSeconds()));
    }

    @Override
    public void delete(String phone) {
        redis.delete(key(phone));
    }

    private String key(String phone) {
        return KEY_PREFIX + phone;
    }
}
