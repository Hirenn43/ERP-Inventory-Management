package com.erp.inventory.service;

import com.erp.inventory.dto.request.MaterialStockMovementRequest;
import com.erp.inventory.dto.response.MaterialStockMovementResponse;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MaterialStockMovementService {
    MaterialStockMovementResponse create(MaterialStockMovementRequest request);
    List<MaterialStockMovementResponse> getAll();
    List<MaterialStockMovementResponse> getByMaterial(Long materialId);

    /** On-hand quantity for one material: Σ(IN) − Σ(OUT), never null. */
    BigDecimal availableQuantity(Long materialId);

    /** On-hand quantity for many materials at once; missing ids map to ZERO. */
    Map<Long, BigDecimal> availableQuantities(Collection<Long> materialIds);
}
