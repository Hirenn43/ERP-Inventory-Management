package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.DesignRequest;
import com.erp.inventory.dto.response.DesignResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Design;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.DesignRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.DesignService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DesignServiceImpl implements DesignService {

    private final DesignRepository designRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public DesignResponse create(DesignRequest request) {
        if (designRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Design already exists with name: " + request.getName());
        }
        Design entity = Design.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .description(request.getDescription())
                .stitch(request.getStitch())
                .rate(request.getRate())
                .designGroup(request.getDesignGroup())
                .closed(request.getClosed())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(designRepository.save(entity));
    }

    @Override
    @Transactional
    public DesignResponse update(Long id, DesignRequest request) {
        Design entity = findEntity(id);
        if (designRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Design already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        entity.setStitch(request.getStitch());
        entity.setRate(request.getRate());
        entity.setDesignGroup(request.getDesignGroup());
        entity.setClosed(request.getClosed());
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
        }
        return toResponse(entity);
    }

    @Override
    public DesignResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<DesignResponse> getAll() {
        return designRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        designRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Design findEntity(Long id) {
        return designRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Design", id));
    }

    private DesignResponse toResponse(Design entity) {
        return DesignResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .description(entity.getDescription())
                .stitch(entity.getStitch())
                .rate(entity.getRate())
                .designGroup(entity.getDesignGroup())
                .closed(entity.getClosed())
                .attributes(entity.getAttributes())
                .build();
    }
}
