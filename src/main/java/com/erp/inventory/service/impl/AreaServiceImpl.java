package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.AreaRequest;
import com.erp.inventory.dto.response.AreaResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Area;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.AreaRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.attributes.AttributeValidator;
import com.erp.inventory.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AreaServiceImpl implements AreaService {

    private final AreaRepository areaRepository;
    private final AttributeValidator attributeValidator;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public AreaResponse create(AreaRequest request) {
        if (areaRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Area already exists with name: " + request.getName());
        }
        Area entity = Area.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .contactMobile(request.getContactMobile())
                .rate(request.getRate())
                .attributes(attributeValidator.validate("areas", request.getAttributes(), null))
                .build();
        return toResponse(areaRepository.save(entity));
    }

    @Override
    @Transactional
    public AreaResponse update(Long id, AreaRequest request) {
        Area entity = findEntity(id);
        if (areaRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Area already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setContactMobile(request.getContactMobile());
        entity.setRate(request.getRate());
        if (request.getAttributes() != null) {
            entity.setAttributes(attributeValidator.validate("areas", request.getAttributes(), id));
        }
        return toResponse(entity);
    }

    @Override
    public AreaResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<AreaResponse> getAll() {
        return areaRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<AreaResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(areaRepository.findAll(TextSearchSpec.contains(q, "name"), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        areaRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Area findEntity(Long id) {
        return areaRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Area", id));
    }

    private AreaResponse toResponse(Area entity) {
        return AreaResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .contactMobile(entity.getContactMobile())
                .rate(entity.getRate())
                .attributes(entity.getAttributes())
                .build();
    }
}
