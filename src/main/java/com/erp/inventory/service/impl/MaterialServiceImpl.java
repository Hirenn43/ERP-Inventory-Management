package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.MaterialRequest;
import com.erp.inventory.dto.response.MaterialResponse;
import com.erp.inventory.entity.Category;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Material;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CategoryRepository;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.MaterialRepository;
import com.erp.inventory.repository.MaterialStockMovementRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.MaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MaterialServiceImpl implements MaterialService {

    private final MaterialRepository materialRepository;
    private final CompanyRepository companyRepository;
    private final CategoryRepository categoryRepository;
    private final MaterialStockMovementRepository movementRepository;

    @Override
    @Transactional
    public MaterialResponse create(MaterialRequest request) {
        if (materialRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Material already exists with code: " + request.getCode());
        }
        Material material = Material.builder()
                .company(resolveCompany(request.getCompanyId()))
                .code(request.getCode())
                .quality(request.getQuality())
                .unit(request.getUnit())
                .name(request.getName())
                .hsnCode(request.getHsnCode())
                .uom(request.getUom())
                .stockType(request.getStockType())
                .gstRate(request.getGstRate())
                .category(resolveCategory(request.getCategoryId()))
                .closed(request.getClosed())
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(materialRepository.save(material));
    }

    @Override
    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = findEntity(id);
        if (materialRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new DuplicateResourceException("Material already exists with code: " + request.getCode());
        }
        material.setCompany(resolveCompany(request.getCompanyId()));
        material.setCode(request.getCode());
        material.setQuality(request.getQuality());
        material.setUnit(request.getUnit());
        material.setName(request.getName());
        material.setHsnCode(request.getHsnCode());
        material.setUom(request.getUom());
        material.setStockType(request.getStockType());
        material.setGstRate(request.getGstRate());
        material.setCategory(resolveCategory(request.getCategoryId()));
        material.setClosed(request.getClosed());
        if (request.getAttributes() != null) {
            material.setAttributes(request.getAttributes());
        }
        return toResponse(material);
    }

    @Override
    public MaterialResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<MaterialResponse> getAll() {
        return materialRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<MaterialResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(materialRepository.findAll(TextSearchSpec.contains(q, "code"), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        materialRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
    }

    private Material findEntity(Long id) {
        return materialRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Material", id));
    }

    private MaterialResponse toResponse(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .companyId(material.getCompany() != null ? material.getCompany().getId() : null)
                .code(material.getCode())
                .quality(material.getQuality())
                .unit(material.getUnit())
                .name(material.getName())
                .hsnCode(material.getHsnCode())
                .uom(material.getUom())
                .stockType(material.getStockType())
                .gstRate(material.getGstRate())
                .categoryId(material.getCategory() != null ? material.getCategory().getId() : null)
                .closed(material.getClosed())
                .availableQty(onHand(material.getId()))
                .attributes(material.getAttributes())
                .build();
    }

    private BigDecimal onHand(Long materialId) {
        if (materialId == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal signed = movementRepository.sumSignedQuantityByMaterialId(materialId);
        return signed != null ? signed : BigDecimal.ZERO;
    }
}
