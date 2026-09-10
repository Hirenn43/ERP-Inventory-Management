package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.ScheduleRequest;
import com.erp.inventory.dto.response.ScheduleResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Schedule;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.ScheduleRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public ScheduleResponse create(ScheduleRequest request) {
        if (scheduleRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Schedule already exists with name: " + request.getName());
        }
        Schedule entity = Schedule.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .parentId(request.getParentId())
                .subGroupRequired(request.getSubGroupRequired())
                .effectOn(request.getEffectOn())
                .scheduleIndex(request.getScheduleIndex())
                .effectOnGp(request.getEffectOnGp())
                .description(request.getDescription())
                .scheduleType(request.getScheduleType())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(scheduleRepository.save(entity));
    }

    @Override
    @Transactional
    public ScheduleResponse update(Long id, ScheduleRequest request) {
        Schedule entity = findEntity(id);
        if (scheduleRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Schedule already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setParentId(request.getParentId());
        entity.setSubGroupRequired(request.getSubGroupRequired());
        entity.setEffectOn(request.getEffectOn());
        entity.setScheduleIndex(request.getScheduleIndex());
        entity.setEffectOnGp(request.getEffectOnGp());
        entity.setDescription(request.getDescription());
        entity.setScheduleType(request.getScheduleType());
        if (request.getAttributes() != null) {
            entity.setAttributes(request.getAttributes());
        }
        return toResponse(entity);
    }

    @Override
    public ScheduleResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<ScheduleResponse> getAll() {
        return scheduleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<ScheduleResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(scheduleRepository.findAll(TextSearchSpec.contains(q, "name"), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        scheduleRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Schedule findEntity(Long id) {
        return scheduleRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Schedule", id));
    }

    private ScheduleResponse toResponse(Schedule entity) {
        return ScheduleResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .parentId(entity.getParentId())
                .subGroupRequired(entity.getSubGroupRequired())
                .effectOn(entity.getEffectOn())
                .scheduleIndex(entity.getScheduleIndex())
                .effectOnGp(entity.getEffectOnGp())
                .description(entity.getDescription())
                .scheduleType(entity.getScheduleType())
                .attributes(entity.getAttributes())
                .build();
    }
}
