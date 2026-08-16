package com.erp.inventory.repository;

import com.erp.inventory.entity.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
    boolean existsByOrderNumber(String orderNumber);
}
