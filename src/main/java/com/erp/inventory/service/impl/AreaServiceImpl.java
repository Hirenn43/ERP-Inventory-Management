package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.AreaRequest;
import com.erp.inventory.dto.response.AreaResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Area;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.AreaRepository;
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
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public AreaResponse create(AreaRequest request) {
        Area entity = Area.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .contactMobile(request.getContactMobile())
                .rate(request.getRate())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(areaRepository.save(entity));
    }

    @Override
    @Transactional
    public AreaResponse update(Long id, AreaRequest request) {
        Area entity = findEntity(id);
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setContactMobile(request.getContactMobile());
        entity.setRate(request.getRate());
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
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
    @Transactional
    public void delete(Long id) {
        areaRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", companyId));
    }

    private Area findEntity(Long id) {
        return areaRepository.findById(id)
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
