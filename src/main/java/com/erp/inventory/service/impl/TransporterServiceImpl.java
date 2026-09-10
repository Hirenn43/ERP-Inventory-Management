package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.TransporterRequest;
import com.erp.inventory.dto.response.TransporterResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Transporter;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.TransporterRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.TransporterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransporterServiceImpl implements TransporterService {

    private final TransporterRepository transporterRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public TransporterResponse create(TransporterRequest request) {
        if (transporterRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Transporter already exists with name: " + request.getName());
        }
        Transporter entity = Transporter.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .address1(request.getAddress1())
                .address2(request.getAddress2())
                .address3(request.getAddress3())
                .address4(request.getAddress4())
                .phone(request.getPhone())
                .contactPerson(request.getContactPerson())
                .gstin(request.getGstin())
                .transportMode(request.getTransportMode())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(transporterRepository.save(entity));
    }

    @Override
    @Transactional
    public TransporterResponse update(Long id, TransporterRequest request) {
        Transporter entity = findEntity(id);
        if (transporterRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Transporter already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setAddress1(request.getAddress1());
        entity.setAddress2(request.getAddress2());
        entity.setAddress3(request.getAddress3());
        entity.setAddress4(request.getAddress4());
        entity.setPhone(request.getPhone());
        entity.setContactPerson(request.getContactPerson());
        entity.setGstin(request.getGstin());
        entity.setTransportMode(request.getTransportMode());
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
        }
        return toResponse(entity);
    }

    @Override
    public TransporterResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<TransporterResponse> getAll() {
        return transporterRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        transporterRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Transporter findEntity(Long id) {
        return transporterRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Transporter", id));
    }

    private TransporterResponse toResponse(Transporter entity) {
        return TransporterResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .address1(entity.getAddress1())
                .address2(entity.getAddress2())
                .address3(entity.getAddress3())
                .address4(entity.getAddress4())
                .phone(entity.getPhone())
                .contactPerson(entity.getContactPerson())
                .gstin(entity.getGstin())
                .transportMode(entity.getTransportMode())
                .attributes(entity.getAttributes())
                .build();
    }
}
