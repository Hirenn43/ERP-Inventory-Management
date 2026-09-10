package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Resource> findOneById(Long id);
}
