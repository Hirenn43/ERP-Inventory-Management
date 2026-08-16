package com.erp.inventory.repository;

import com.erp.inventory.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
}
