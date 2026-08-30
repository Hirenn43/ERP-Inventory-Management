package com.erp.inventory.repository;

import com.erp.inventory.entity.Material;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaterialRepository extends JpaRepository<Material, Long> {
    boolean existsByCode(String code);
}
