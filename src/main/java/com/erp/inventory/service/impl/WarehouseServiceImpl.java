package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.WarehouseRequest;
import com.erp.inventory.dto.response.WarehouseResponse;
import com.erp.inventory.entity.Warehouse;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.WarehouseRepository;
import com.erp.inventory.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public WarehouseResponse create(WarehouseRequest request) {
        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .build();
        return toResponse(warehouseRepository.save(warehouse));
    }

    @Override
    @Transactional
    public WarehouseResponse update(Long id, WarehouseRequest request) {
        Warehouse warehouse = findEntity(id);
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
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
                .build();
    }
}
