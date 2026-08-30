package com.erp.inventory.repository;

import com.erp.inventory.entity.OrderFulfillmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderFulfillmentEventRepository extends JpaRepository<OrderFulfillmentEvent, Long> {
    List<OrderFulfillmentEvent> findByOrderIdOrderByDateAsc(Long orderId);
}
