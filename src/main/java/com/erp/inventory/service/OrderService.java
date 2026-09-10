package com.erp.inventory.service;

import com.erp.inventory.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.request.OrderRequest;
import com.erp.inventory.dto.response.OrderMaterialRequirementResponse;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.security.UserPrincipal;

import java.math.BigDecimal;
import java.util.List;

public interface OrderService {
    OrderResponse create(OrderRequest request, UserPrincipal actor);
    OrderResponse getById(Long id);
    List<OrderResponse> getAll();
    PageResponse<OrderResponse> getPage(Pageable pageable, String q);
    OrderResponse addFulfillmentEvent(Long orderId, OrderFulfillmentEventRequest request);

    /** Yarn a hypothetical order of {@code qty} units of the product would consume, vs current stock. */
    OrderMaterialRequirementResponse computeRequirement(Long productId, Long componentId, BigDecimal qty);
}
