package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.ResourceRequest;
import com.erp.inventory.dto.response.ResourceResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Resource;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.ResourceRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl implements ResourceService {

    private final ResourceRepository resourceRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        Resource resource = Resource.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(resourceRepository.save(resource));
    }

    @Override
    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        Resource resource = findEntity(id);
        resource.setCompany(resolveCompany(request.getCompanyId()));
        resource.setName(request.getName());
        if (request.getAttributes() != null) {
            resource.setAttributes(request.getAttributes());
        }
        return toResponse(resource);
    }

    @Override
    public ResourceResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<ResourceResponse> getAll() {
        return resourceRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        resourceRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Resource findEntity(Long id) {
        return resourceRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Resource", id));
    }

    private ResourceResponse toResponse(Resource resource) {
        return ResourceResponse.builder()
                .id(resource.getId())
                .companyId(resource.getCompany() != null ? resource.getCompany().getId() : null)
                .name(resource.getName())
                .attributes(resource.getAttributes())
                .build();
    }
}
