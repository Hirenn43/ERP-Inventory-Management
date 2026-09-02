package com.erp.inventory.service;

import com.erp.inventory.dto.request.DesignRequest;
import com.erp.inventory.dto.response.DesignResponse;

import java.util.List;

public interface DesignService {
    DesignResponse create(DesignRequest request);
    DesignResponse update(Long id, DesignRequest request);
    DesignResponse getById(Long id);
    List<DesignResponse> getAll();
    void delete(Long id);
}
