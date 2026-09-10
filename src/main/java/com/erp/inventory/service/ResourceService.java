package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.ResourceRequest;
import com.erp.inventory.dto.response.ResourceResponse;

import java.util.List;

public interface ResourceService {
    ResourceResponse create(ResourceRequest request);
    ResourceResponse update(Long id, ResourceRequest request);
    ResourceResponse getById(Long id);
    List<ResourceResponse> getAll();
    PageResponse<ResourceResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
