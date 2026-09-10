package com.erp.inventory.repository;

import com.erp.inventory.entity.ProcessedWebhookMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookMessageRepository extends JpaRepository<ProcessedWebhookMessage, String> {
}
