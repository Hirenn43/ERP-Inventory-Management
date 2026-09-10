package com.erp.inventory.repository;

import java.util.Optional;
import com.erp.inventory.entity.Party;
import com.erp.inventory.entity.enums.PartyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface PartyRepository extends JpaRepository<Party, Long>, JpaSpecificationExecutor<Party> {
    List<Party> findByRolesContaining(PartyRole role);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<Party> findOneById(Long id);
}
