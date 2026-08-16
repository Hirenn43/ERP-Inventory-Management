package com.erp.inventory.repository;

import com.erp.inventory.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    boolean existsByOrderNumber(String orderNumber);
}
