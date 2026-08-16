package com.erp.inventory.service;

import com.erp.inventory.dto.request.StockAdjustmentRequest;
import com.erp.inventory.dto.response.InventoryResponse;
import com.erp.inventory.dto.response.StockMovementResponse;
import com.erp.inventory.security.UserPrincipal;

import java.util.List;

public interface InventoryService {
    List<InventoryResponse> getAll();
    List<InventoryResponse> getByProduct(Long productId);
    List<InventoryResponse> getByWarehouse(Long warehouseId);
    StockMovementResponse adjustStock(StockAdjustmentRequest request, UserPrincipal actor);
    List<StockMovementResponse> getMovementsForProduct(Long productId);
}
