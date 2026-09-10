package com.erp.inventory.service.whatsapp;

import com.erp.inventory.entity.ProcessedWebhookMessage;
import com.erp.inventory.repository.ProcessedWebhookMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Webhook message idempotency. Each method runs in its own committed transaction so a claim is
 * durable the instant it is taken - independent of whatever the caller does next.
 */
@Service
@RequiredArgsConstructor
public class WebhookIdempotencyService {

    private final ProcessedWebhookMessageRepository repository;

    /**
     * Atomically claims a message id for processing.
     *
     * @return {@code true} if this call took the claim (caller should process the message);
     *         {@code false} if it was already claimed (caller should skip - a retry or a
     *         concurrent delivery).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryClaim(String messageId) {
        if (repository.existsById(messageId)) {
            return false;
        }
        try {
            repository.saveAndFlush(new ProcessedWebhookMessage(messageId, Instant.now()));
            return true;
        } catch (DataIntegrityViolationException raceLost) {
            return false;
        }
    }

    /**
     * Releases a claim so Meta's redelivery can retry the message. Called when processing failed
     * after the claim was taken.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void release(String messageId) {
        repository.deleteById(messageId);
    }
}
