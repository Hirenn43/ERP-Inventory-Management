package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Design;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DesignRepository extends JpaRepository<Design, Long>, JpaSpecificationExecutor<Design> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Design> findOneById(Long id);
}
