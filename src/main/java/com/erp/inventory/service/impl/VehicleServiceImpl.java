package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.VehicleRequest;
import com.erp.inventory.dto.response.VehicleResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Vehicle;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.VehicleRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.attributes.AttributeValidator;
import com.erp.inventory.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final AttributeValidator attributeValidator;
    private final CompanyRepository companyRepository;

    @Override
    @Transactional
    public VehicleResponse create(VehicleRequest request) {
        if (vehicleRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Vehicle already exists with name: " + request.getName());
        }
        Vehicle entity = Vehicle.builder()
                .company(resolveCompany(request.getCompanyId()))
                .name(request.getName())
                .ownerName(request.getOwnerName())
                .mobileNo(request.getMobileNo())
                .contactPerson(request.getContactPerson())
                .contactMobile(request.getContactMobile())
                .description(request.getDescription())
                .tdsPct(request.getTdsPct())
                .loadingCharge(request.getLoadingCharge())
                .closed(request.getClosed())
                .attributes(attributeValidator.validate("vehicles", request.getAttributes(), null))
                .build();
        return toResponse(vehicleRepository.save(entity));
    }

    @Override
    @Transactional
    public VehicleResponse update(Long id, VehicleRequest request) {
        Vehicle entity = findEntity(id);
        if (vehicleRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Vehicle already exists with name: " + request.getName());
        }
        entity.setCompany(resolveCompany(request.getCompanyId()));
        entity.setName(request.getName());
        entity.setOwnerName(request.getOwnerName());
        entity.setMobileNo(request.getMobileNo());
        entity.setContactPerson(request.getContactPerson());
        entity.setContactMobile(request.getContactMobile());
        entity.setDescription(request.getDescription());
        entity.setTdsPct(request.getTdsPct());
        entity.setLoadingCharge(request.getLoadingCharge());
        entity.setClosed(request.getClosed());
        if (request.getAttributes() != null) {
            entity.setAttributes(attributeValidator.validate("vehicles", request.getAttributes(), id));
        }
        return toResponse(entity);
    }

    @Override
    public VehicleResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<VehicleResponse> getAll() {
        return vehicleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<VehicleResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(vehicleRepository.findAll(TextSearchSpec.contains(q, "name"), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        vehicleRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Vehicle findEntity(Long id) {
        return vehicleRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Vehicle", id));
    }

    private VehicleResponse toResponse(Vehicle entity) {
        return VehicleResponse.builder()
                .id(entity.getId())
                .companyId(entity.getCompany() != null ? entity.getCompany().getId() : null)
                .name(entity.getName())
                .ownerName(entity.getOwnerName())
                .mobileNo(entity.getMobileNo())
                .contactPerson(entity.getContactPerson())
                .contactMobile(entity.getContactMobile())
                .description(entity.getDescription())
                .tdsPct(entity.getTdsPct())
                .loadingCharge(entity.getLoadingCharge())
                .closed(entity.getClosed())
                .attributes(entity.getAttributes())
                .build();
    }
}
