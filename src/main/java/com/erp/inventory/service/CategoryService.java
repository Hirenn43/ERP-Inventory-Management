package com.erp.inventory.service;

import com.erp.inventory.dto.request.CategoryRequest;
import com.erp.inventory.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    CategoryResponse getById(Long id);
    List<CategoryResponse> getAll();
    void delete(Long id);
}
