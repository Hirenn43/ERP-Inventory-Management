package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TransporterRepository extends JpaRepository<Transporter, Long>, JpaSpecificationExecutor<Transporter> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Transporter> findOneById(Long id);
}
