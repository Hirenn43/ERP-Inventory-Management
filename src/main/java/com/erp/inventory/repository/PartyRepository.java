package com.erp.inventory.repository;

import com.erp.inventory.entity.Party;
import com.erp.inventory.entity.enums.PartyRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PartyRepository extends JpaRepository<Party, Long> {
    List<Party> findByRolesContaining(PartyRole role);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
