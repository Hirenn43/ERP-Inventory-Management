package com.erp.inventory.repository;

import com.erp.inventory.entity.WhatsappUserLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WhatsappUserLinkRepository extends JpaRepository<WhatsappUserLink, Long> {

    Optional<WhatsappUserLink> findByPhoneAndEnabledTrue(String phone);

    Optional<WhatsappUserLink> findByPhone(String phone);

    boolean existsByPhone(String phone);

    /** Tenant-filtered id lookup (JpaRepository.findById bypasses Hibernate @Filter). */
    Optional<WhatsappUserLink> findOneById(Long id);
}
