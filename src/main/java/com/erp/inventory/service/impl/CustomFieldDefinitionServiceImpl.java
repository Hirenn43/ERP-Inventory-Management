package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.CustomFieldDefinitionRequest;
import com.erp.inventory.dto.response.CustomFieldDefinitionResponse;
import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.CustomFieldDefinition;
import com.erp.inventory.entity.enums.FieldDataType;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.CustomFieldDefinitionRepository;
import com.erp.inventory.repository.spec.TextSearchSpec;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.CustomFieldDefinitionService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomFieldDefinitionServiceImpl implements CustomFieldDefinitionService {

    private final CustomFieldDefinitionRepository repository;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public CustomFieldDefinitionResponse create(CustomFieldDefinitionRequest request) {
        if (repository.existsByResourceAndFieldKeyIgnoreCase(request.getResource(), request.getFieldKey())) {
            throw new DuplicateResourceException(
                    "A field '" + request.getFieldKey() + "' already exists for " + request.getResource());
        }
        validate(request);
        CustomFieldDefinition entity = CustomFieldDefinition.builder()
                .company(currentCompany())
                .resource(request.getResource().trim())
                .fieldKey(request.getFieldKey().trim())
                .label(request.getLabel().trim())
                .dataType(request.getDataType())
                .maxLength(request.getMaxLength())
                .minValue(request.getMinValue())
                .maxValue(request.getMaxValue())
                .required(request.isRequired())
                .uniqueValue(request.isUniqueValue())
                .section(trimToNull(request.getSection()))
                .displayOrder(request.getDisplayOrder() != null
                        ? request.getDisplayOrder()
                        : nextDisplayOrder(request.getResource()))
                .options(request.getOptions() != null ? new ArrayList<>(request.getOptions()) : new ArrayList<>())
                .active(request.getActive() == null || request.getActive())
                .build();
        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public CustomFieldDefinitionResponse update(Long id, CustomFieldDefinitionRequest request) {
        CustomFieldDefinition entity = findEntity(id);
        if (repository.existsByResourceAndFieldKeyIgnoreCaseAndIdNot(
                request.getResource(), request.getFieldKey(), id)) {
            throw new DuplicateResourceException(
                    "A field '" + request.getFieldKey() + "' already exists for " + request.getResource());
        }
        validate(request);
        entity.setResource(request.getResource().trim());
        entity.setFieldKey(request.getFieldKey().trim());
        entity.setLabel(request.getLabel().trim());
        entity.setDataType(request.getDataType());
        entity.setMaxLength(request.getMaxLength());
        entity.setMinValue(request.getMinValue());
        entity.setMaxValue(request.getMaxValue());
        entity.setRequired(request.isRequired());
        entity.setUniqueValue(request.isUniqueValue());
        entity.setSection(trimToNull(request.getSection()));
        if (request.getDisplayOrder() != null) {
            entity.setDisplayOrder(request.getDisplayOrder());
        }
        entity.setOptions(request.getOptions() != null ? new ArrayList<>(request.getOptions()) : new ArrayList<>());
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
        return toResponse(entity);
    }

    @Override
    public CustomFieldDefinitionResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<CustomFieldDefinitionResponse> getAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<CustomFieldDefinitionResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(
                repository.findAll(TextSearchSpec.contains(q, "label", "fieldKey", "resource"), pageable)
                        .map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.delete(findEntity(id));
    }

    @Override
    public List<CustomFieldDefinitionResponse> getByResource(String resource, boolean includeInactive) {
        var list = includeInactive
                ? repository.findByResourceOrderByDisplayOrderAscIdAsc(resource)
                : repository.findByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(resource);
        return list.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public List<CustomFieldDefinitionResponse> seedDefaults(String resource, List<CustomFieldDefinitionRequest> defs) {
        int order = nextDisplayOrder(resource);
        List<CustomFieldDefinitionResponse> created = new ArrayList<>();
        for (CustomFieldDefinitionRequest d : defs) {
            if (repository.existsByResourceAndFieldKeyIgnoreCase(resource, d.getFieldKey())) {
                continue;
            }
            d.setResource(resource);
            if (d.getDisplayOrder() == null) {
                d.setDisplayOrder(order++);
            }
            created.add(create(d));
        }
        return created;
    }

    // --- helpers ---------------------------------------------------------

    private void validate(CustomFieldDefinitionRequest r) {
        if (r.getDataType() == FieldDataType.SELECT
                && (r.getOptions() == null || r.getOptions().isEmpty())) {
            throw new IllegalArgumentException("A dropdown field needs at least one option");
        }
    }

    private int nextDisplayOrder(String resource) {
        return repository.findByResourceOrderByDisplayOrderAscIdAsc(resource).stream()
                .mapToInt(CustomFieldDefinition::getDisplayOrder)
                .max().orElse(-1) + 1;
    }

    private Company currentCompany() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new IllegalStateException("No tenant in context");
        }
        return companyRepository.getReferenceById(tenantId);
    }

    private CustomFieldDefinition findEntity(Long id) {
        return repository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Custom field", id));
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private CustomFieldDefinitionResponse toResponse(CustomFieldDefinition e) {
        return CustomFieldDefinitionResponse.builder()
                .id(e.getId())
                .resource(e.getResource())
                .fieldKey(e.getFieldKey())
                .label(e.getLabel())
                .dataType(e.getDataType())
                .maxLength(e.getMaxLength())
                .minValue(e.getMinValue())
                .maxValue(e.getMaxValue())
                .required(e.isRequired())
                .uniqueValue(e.isUniqueValue())
                .section(e.getSection())
                .displayOrder(e.getDisplayOrder())
                .options(e.getOptions())
                .active(e.isActive())
                .build();
    }
}
