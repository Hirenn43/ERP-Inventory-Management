package com.erp.inventory.service;

import com.erp.inventory.dto.request.SupplierRequest;
import com.erp.inventory.dto.response.SupplierResponse;

import java.util.List;

public interface SupplierService {
    SupplierResponse create(SupplierRequest request);
    SupplierResponse update(Long id, SupplierRequest request);
    SupplierResponse getById(Long id);
    List<SupplierResponse> getAll();
    void delete(Long id);
}
