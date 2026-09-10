package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.CompanyRequest;
import com.erp.inventory.dto.response.CompanyResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.CompanyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public CompanyResponse create(CompanyRequest request) {
        Company company = Company.builder()
                .name(request.getName())
                .address(request.getAddress())
                .mobile(request.getMobile())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(companyRepository.save(company));
    }

    @Override
    @Transactional
    public CompanyResponse update(Long id, CompanyRequest request) {
        Company company = findEntity(id);
        company.setName(request.getName());
        company.setAddress(request.getAddress());
        company.setMobile(request.getMobile());
        if (request.getAttributes() != null) {
            company.setAttributes(request.getAttributes());
        }
        return toResponse(company);
    }

    @Override
    public CompanyResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<CompanyResponse> getAll() {
        // A tenant only ever sees its own company.
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return List.of();
        }
        return companyRepository.findById(tenantId).map(this::toResponse).map(List::of).orElseGet(List::of);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        companyRepository.delete(findEntity(id));
    }

    private Company findEntity(Long id) {
        Long tenantId = TenantContext.get();
        if (tenantId != null && !tenantId.equals(id)) {
            throw ResourceNotFoundException.of("Company", id);
        }
        return companyRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", id));
    }

    private CompanyResponse toResponse(Company company) {
        return CompanyResponse.builder()
                .id(company.getId())
                .name(company.getName())
                .address(company.getAddress())
                .mobile(company.getMobile())
                .attributes(company.getAttributes())
                .build();
    }
}
