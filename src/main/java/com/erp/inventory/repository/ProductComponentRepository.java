package com.erp.inventory.repository;

import com.erp.inventory.entity.ProductComponent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductComponentRepository extends JpaRepository<ProductComponent, Long> {
    List<ProductComponent> findByProductId(Long productId);
}
