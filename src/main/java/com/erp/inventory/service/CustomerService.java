package com.erp.inventory.service;

import com.erp.inventory.dto.request.CustomerRequest;
import com.erp.inventory.dto.response.CustomerResponse;

import java.util.List;

public interface CustomerService {
    CustomerResponse create(CustomerRequest request);
    CustomerResponse update(Long id, CustomerRequest request);
    CustomerResponse getById(Long id);
    List<CustomerResponse> getAll();
    void delete(Long id);
}
