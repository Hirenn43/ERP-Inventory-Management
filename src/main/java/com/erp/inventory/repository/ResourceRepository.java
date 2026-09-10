package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ResourceRepository extends JpaRepository<Resource, Long>, JpaSpecificationExecutor<Resource> {

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Resource> findOneById(Long id);
}
