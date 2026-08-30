package com.erp.inventory.service;

import com.erp.inventory.dto.request.MaterialRequest;
import com.erp.inventory.dto.response.MaterialResponse;

import java.util.List;

public interface MaterialService {
    MaterialResponse create(MaterialRequest request);
    MaterialResponse update(Long id, MaterialRequest request);
    MaterialResponse getById(Long id);
    List<MaterialResponse> getAll();
    void delete(Long id);
}
