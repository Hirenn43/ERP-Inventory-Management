package com.erp.inventory.repository;

import com.erp.inventory.entity.Role;
import com.erp.inventory.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
