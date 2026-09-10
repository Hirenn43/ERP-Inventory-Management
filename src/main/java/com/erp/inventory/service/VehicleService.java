package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.VehicleRequest;
import com.erp.inventory.dto.response.VehicleResponse;

import java.util.List;

public interface VehicleService {
    VehicleResponse create(VehicleRequest request);
    VehicleResponse update(Long id, VehicleRequest request);
    VehicleResponse getById(Long id);
    List<VehicleResponse> getAll();
    PageResponse<VehicleResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
