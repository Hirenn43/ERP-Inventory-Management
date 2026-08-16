package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.SupplierRequest;
import com.erp.inventory.dto.response.SupplierResponse;
import com.erp.inventory.entity.Supplier;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.SupplierRepository;
import com.erp.inventory.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        Supplier supplier = Supplier.builder()
                .name(request.getName())
                .contactPerson(request.getContactPerson())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .build();
        return toResponse(supplierRepository.save(supplier));
    }

    @Override
    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = findEntity(id);
        supplier.setName(request.getName());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setPhone(request.getPhone());
        supplier.setEmail(request.getEmail());
        supplier.setAddress(request.getAddress());
        return toResponse(supplier);
    }

    @Override
    public SupplierResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        supplierRepository.delete(findEntity(id));
    }

    private Supplier findEntity(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .contactPerson(supplier.getContactPerson())
                .phone(supplier.getPhone())
                .email(supplier.getEmail())
                .address(supplier.getAddress())
                .build();
    }
}
