package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.MaterialRequest;
import com.erp.inventory.dto.response.MaterialResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Material;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.MaterialRepository;
import com.erp.inventory.repository.MaterialStockMovementRepository;
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
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
                .build();
        return toResponse(materialRepository.save(material));
    }

    @Override
    @Transactional
    public MaterialResponse update(Long id, MaterialRequest request) {
        Material material = findEntity(id);
        material.setCompany(resolveCompany(request.getCompanyId()));
        material.setCode(request.getCode());
        material.setQuality(request.getQuality());
        material.setUnit(request.getUnit());
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
    @Transactional
    public void delete(Long id) {
        materialRepository.delete(findEntity(id));
    }

    private Company resolveCompany(Long companyId) {
        if (companyId == null) {
            return null;
        }
        return companyRepository.findById(companyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Company", companyId));
    }

    private Material findEntity(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Material", id));
    }

    private MaterialResponse toResponse(Material material) {
        return MaterialResponse.builder()
                .id(material.getId())
                .companyId(material.getCompany() != null ? material.getCompany().getId() : null)
                .code(material.getCode())
                .quality(material.getQuality())
                .unit(material.getUnit())
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
