package com.erp.inventory.repository;

import com.erp.inventory.entity.User;
import com.erp.inventory.entity.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<User> findOneById(Long id);

    long countByRoles_NameAndCompany_IdAndEnabledTrue(RoleName roleName, Long companyId);
}
