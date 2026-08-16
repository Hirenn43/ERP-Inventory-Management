package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.PurchaseOrderItemRequest;
import com.erp.inventory.dto.request.PurchaseOrderRequest;
import com.erp.inventory.dto.response.PurchaseOrderItemResponse;
import com.erp.inventory.dto.response.PurchaseOrderResponse;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.PurchaseOrder;
import com.erp.inventory.entity.PurchaseOrderItem;
import com.erp.inventory.entity.Supplier;
import com.erp.inventory.entity.Warehouse;
import com.erp.inventory.entity.enums.OrderStatus;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.PurchaseOrderRepository;
import com.erp.inventory.repository.SupplierRepository;
import com.erp.inventory.repository.WarehouseRepository;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final com.erp.inventory.service.InventoryService inventoryService;

    @Override
    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request, UserPrincipal actor) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", request.getSupplierId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Warehouse", request.getWarehouseId()));

        PurchaseOrder order = PurchaseOrder.builder()
                .orderNumber(generateOrderNumber())
                .supplier(supplier)
                .warehouse(warehouse)
                .status(OrderStatus.PENDING)
                .createdBy(actor != null ? actor.getUser() : null)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", itemRequest.getProductId()));

            PurchaseOrderItem item = PurchaseOrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitCost(itemRequest.getUnitCost())
                    .build();
            order.addItem(item);

            total = total.add(itemRequest.getUnitCost().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
        }
        order.setTotalAmount(total);

        return toResponse(purchaseOrderRepository.save(order));
    }

    @Override
    public PurchaseOrderResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<PurchaseOrderResponse> getAll() {
        return purchaseOrderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public PurchaseOrderResponse complete(Long id) {
        PurchaseOrder order = findEntity(id);
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Purchase order is already " + order.getStatus());
        }

        for (PurchaseOrderItem item : order.getItems()) {
            com.erp.inventory.dto.request.StockAdjustmentRequest adjustment =
                    new com.erp.inventory.dto.request.StockAdjustmentRequest();
            adjustment.setProductId(item.getProduct().getId());
            adjustment.setWarehouseId(order.getWarehouse().getId());
            adjustment.setType(com.erp.inventory.entity.enums.MovementType.STOCK_IN);
            adjustment.setQuantity(item.getQuantity());
            adjustment.setRemarks("Purchase order " + order.getOrderNumber() + " received");
            inventoryService.adjustStock(adjustment, order.getCreatedBy() != null
                    ? new UserPrincipal(order.getCreatedBy()) : null);
        }

        order.setStatus(OrderStatus.COMPLETED);
        return toResponse(order);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancel(Long id) {
        PurchaseOrder order = findEntity(id);
        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("Completed purchase orders cannot be cancelled");
        }
        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order);
    }

    private PurchaseOrder findEntity(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("PurchaseOrder", id));
    }

    private String generateOrderNumber() {
        return "PO-" + Instant.now().toEpochMilli();
    }

    private PurchaseOrderResponse toResponse(PurchaseOrder order) {
        List<PurchaseOrderItemResponse> items = order.getItems().stream()
                .map(item -> PurchaseOrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitCost(item.getUnitCost())
                        .build())
                .toList();

        return PurchaseOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .supplierName(order.getSupplier().getName())
                .warehouseName(order.getWarehouse().getName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdBy(order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
