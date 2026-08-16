package com.erp.inventory.service;

import com.erp.inventory.dto.request.SalesOrderRequest;
import com.erp.inventory.dto.response.SalesOrderResponse;
import com.erp.inventory.security.UserPrincipal;

import java.util.List;

public interface SalesOrderService {
    SalesOrderResponse create(SalesOrderRequest request, UserPrincipal actor);
    SalesOrderResponse getById(Long id);
    List<SalesOrderResponse> getAll();
    SalesOrderResponse cancel(Long id, UserPrincipal actor);
}
