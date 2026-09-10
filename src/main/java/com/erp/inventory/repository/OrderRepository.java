package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    boolean existsByOrderNumber(String orderNumber);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Order> findOneById(Long id);
}
