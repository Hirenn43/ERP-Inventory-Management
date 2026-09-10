package com.erp.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** One row per WhatsApp webhook message id we have already handled. Meta redelivers a payload
 *  when it doesn't get a prompt 200, so the id is checked before {@code handleInbound} runs to
 *  avoid creating a second order / party on retry. */
@Entity
@Table(name = "processed_webhook_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedWebhookMessage {

    /** Meta's message id (wamid...). */
    @Id
    @Column(length = 128, nullable = false, updatable = false)
    private String messageId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt;
}
