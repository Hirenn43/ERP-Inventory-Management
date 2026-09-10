package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.WarehouseRequest;
import com.erp.inventory.dto.response.WarehouseResponse;
import com.erp.inventory.entity.Warehouse;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.WarehouseRepository;
import com.erp.inventory.service.attributes.AttributeValidator;
import com.erp.inventory.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final AttributeValidator attributeValidator;

    @Override
    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .attributes(attributeValidator.validate("warehouses", request.getAttributes(), null))
                .build();
        return toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = findEntity(id);
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        if (request.getAttributes() != null) {
            warehouse.setAttributes(attributeValidator.validate("warehouses", request.getAttributes(), id));
        }
        return toResponse(warehouse);
    }

    @Override
    public WarehouseResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<WarehouseResponse> getAll() {
        return warehouseRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<WarehouseResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(warehouseRepository.findAll(TextSearchSpec.contains(q, "name"), pageable).map(this::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        warehouseRepository.delete(findEntity(id));
    }

    private Warehouse findEntity(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Warehouse", id));
    }

    private WarehouseResponse toResponse(Warehouse warehouse) {
        return WarehouseResponse.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .location(warehouse.getLocation())
                .attributes(warehouse.getAttributes())
                .build();
    }
}
