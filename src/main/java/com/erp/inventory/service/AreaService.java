package com.erp.inventory.service;

import com.erp.inventory.dto.request.AreaRequest;
import com.erp.inventory.dto.response.AreaResponse;

import java.util.List;

public interface AreaService {
    AreaResponse create(AreaRequest request);
    AreaResponse update(Long id, AreaRequest request);
    AreaResponse getById(Long id);
    List<AreaResponse> getAll();
    void delete(Long id);
}
