package com.erp.inventory.repository;

import com.erp.inventory.entity.Transporter;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransporterRepository extends JpaRepository<Transporter, Long> {
    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
