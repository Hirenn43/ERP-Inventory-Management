package com.erp.inventory.repository;

import com.erp.inventory.entity.CustomFieldDefinition;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CustomFieldDefinitionRepository
        extends JpaRepository<CustomFieldDefinition, Long>, JpaSpecificationExecutor<CustomFieldDefinition> {

    List<CustomFieldDefinition> findByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(String resource);

    List<CustomFieldDefinition> findByResourceOrderByDisplayOrderAscIdAsc(String resource);

    boolean existsByResourceAndFieldKeyIgnoreCase(String resource, String fieldKey);

    boolean existsByResourceAndFieldKeyIgnoreCaseAndIdNot(String resource, String fieldKey, Long id);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<CustomFieldDefinition> findOneById(Long id);
}
