package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.WarehouseRequest;
import com.erp.inventory.dto.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    WarehouseResponse create(WarehouseRequest request);
    WarehouseResponse update(Long id, WarehouseRequest request);
    WarehouseResponse getById(Long id);
    List<WarehouseResponse> getAll();
    PageResponse<WarehouseResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
