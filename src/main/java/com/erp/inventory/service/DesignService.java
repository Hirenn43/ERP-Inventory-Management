package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.DesignRequest;
import com.erp.inventory.dto.response.DesignResponse;

import java.util.List;

public interface DesignService {
    DesignResponse create(DesignRequest request);
    DesignResponse update(Long id, DesignRequest request);
    DesignResponse getById(Long id);
    List<DesignResponse> getAll();
    PageResponse<DesignResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
