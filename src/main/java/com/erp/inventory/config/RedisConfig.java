package com.erp.inventory.config;

import com.erp.inventory.service.whatsapp.WaConversation;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, WaConversation> waConversationRedisTemplate(RedisConnectionFactory cf,
                                                                            ObjectMapper objectMapper) {
        Jackson2JsonRedisSerializer<WaConversation> valueSerializer =
                new Jackson2JsonRedisSerializer<>(objectMapper, WaConversation.class);

        RedisTemplate<String, WaConversation> template = new RedisTemplate<>();
        template.setConnectionFactory(cf);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(valueSerializer);
        template.afterPropertiesSet();
        return template;
    }
}
