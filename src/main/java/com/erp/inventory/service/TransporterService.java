package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.TransporterRequest;
import com.erp.inventory.dto.response.TransporterResponse;

import java.util.List;

public interface TransporterService {
    TransporterResponse create(TransporterRequest request);
    TransporterResponse update(Long id, TransporterRequest request);
    TransporterResponse getById(Long id);
    List<TransporterResponse> getAll();
    PageResponse<TransporterResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
