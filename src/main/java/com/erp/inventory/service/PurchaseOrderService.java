package com.erp.inventory.service;

import com.erp.inventory.dto.request.PurchaseOrderRequest;
import com.erp.inventory.dto.response.PurchaseOrderResponse;
import com.erp.inventory.security.UserPrincipal;

import java.util.List;

public interface PurchaseOrderService {
    PurchaseOrderResponse create(PurchaseOrderRequest request, UserPrincipal actor);
    PurchaseOrderResponse getById(Long id);
    List<PurchaseOrderResponse> getAll();
    PurchaseOrderResponse complete(Long id);
    PurchaseOrderResponse cancel(Long id);
}
