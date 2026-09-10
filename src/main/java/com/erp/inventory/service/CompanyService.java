package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.CompanyRequest;
import com.erp.inventory.dto.response.CompanyResponse;

import java.util.List;

public interface CompanyService {
    CompanyResponse create(CompanyRequest request);
    CompanyResponse update(Long id, CompanyRequest request);
    CompanyResponse getById(Long id);
    List<CompanyResponse> getAll();
    PageResponse<CompanyResponse> getPage(Pageable pageable, String q);
    void delete(Long id);
}
