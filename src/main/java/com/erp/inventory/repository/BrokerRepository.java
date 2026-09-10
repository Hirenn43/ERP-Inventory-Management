package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Broker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BrokerRepository extends JpaRepository<Broker, Long>, JpaSpecificationExecutor<Broker> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Broker> findOneById(Long id);
}
