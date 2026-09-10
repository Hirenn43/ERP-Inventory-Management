package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Schedule> findOneById(Long id);
}
