package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.WhatsappUserLinkRequest;
import com.erp.inventory.dto.response.WhatsappUserLinkResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.WhatsappUserLink;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.UserRepository;
import com.erp.inventory.repository.WhatsappUserLinkRepository;
import com.erp.inventory.service.WhatsappUserLinkService;
import com.erp.inventory.service.whatsapp.PhoneNumbers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WhatsappUserLinkServiceImpl implements WhatsappUserLinkService {

    private final WhatsappUserLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public WhatsappUserLinkResponse create(WhatsappUserLinkRequest request) {
        String phone = PhoneNumbers.digits(request.getPhone());
        if (linkRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("A link already exists for phone " + phone);
        }
        WhatsappUserLink link = WhatsappUserLink.builder()
                .phone(phone)
                .user(resolveUser(request.getUserId()))
                .company(resolveCompany(request.getCompanyId()))
                .enabled(request.getEnabled() == null || request.getEnabled())
                .build();
        return toResponse(linkRepository.save(link));
    }

    @Override
    @Transactional
    public WhatsappUserLinkResponse update(Long id, WhatsappUserLinkRequest request) {
        WhatsappUserLink link = findEntity(id);
        String phone = PhoneNumbers.digits(request.getPhone());
        linkRepository.findByPhone(phone)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("A link already exists for phone " + phone);
                });
        link.setPhone(phone);
        link.setUser(resolveUser(request.getUserId()));
        link.setCompany(resolveCompany(request.getCompanyId()));
        if (request.getEnabled() != null) {
            link.setEnabled(request.getEnabled());
        }
        return toResponse(link);
    }

    @Override
    public WhatsappUserLinkResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<WhatsappUserLinkResponse> getAll() {
        return linkRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        linkRepository.delete(findEntity(id));
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
    }

    private Company resolveCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", companyId));
    }

    private WhatsappUserLink findEntity(Long id) {
        return linkRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("WhatsappUserLink", id));
    }

    private WhatsappUserLinkResponse toResponse(WhatsappUserLink link) {
        return WhatsappUserLinkResponse.builder()
                .id(link.getId())
                .phone(link.getPhone())
                .userId(link.getUser().getId())
                .username(link.getUser().getUsername())
                .companyId(link.getCompany() != null ? link.getCompany().getId() : null)
                .companyName(link.getCompany() != null ? link.getCompany().getName() : null)
                .enabled(link.isEnabled())
                .createdAt(link.getCreatedAt())
                .build();
    }
}
