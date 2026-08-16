package com.erp.inventory.repository;

import com.erp.inventory.entity.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
}
