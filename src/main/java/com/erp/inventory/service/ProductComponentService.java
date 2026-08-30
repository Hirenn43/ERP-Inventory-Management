package com.erp.inventory.service;

import com.erp.inventory.dto.request.ProductComponentRequest;
import com.erp.inventory.dto.response.ProductComponentResponse;

import java.util.List;

public interface ProductComponentService {
    ProductComponentResponse create(ProductComponentRequest request);
    List<ProductComponentResponse> getByProduct(Long productId);
    void delete(Long id);
}
