package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.SalesOrderItemRequest;
import com.erp.inventory.dto.request.SalesOrderRequest;
import com.erp.inventory.dto.request.StockAdjustmentRequest;
import com.erp.inventory.dto.response.SalesOrderItemResponse;
import com.erp.inventory.dto.response.SalesOrderResponse;
import com.erp.inventory.entity.Customer;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.SalesOrder;
import com.erp.inventory.entity.SalesOrderItem;
import com.erp.inventory.entity.Warehouse;
import com.erp.inventory.entity.enums.MovementType;
import com.erp.inventory.entity.enums.OrderStatus;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CustomerRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.SalesOrderRepository;
import com.erp.inventory.repository.WarehouseRepository;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.InventoryService;
import com.erp.inventory.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SalesOrderServiceImpl implements SalesOrderService {

    private final SalesOrderRepository salesOrderRepository;
    private final CustomerRepository customerRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;

    @Override
    @Transactional
    public SalesOrderResponse create(SalesOrderRequest request, UserPrincipal actor) {
        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", request.getCustomerId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Warehouse", request.getWarehouseId()));

        SalesOrder order = SalesOrder.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .warehouse(warehouse)
                .status(OrderStatus.PENDING)
                .createdBy(actor != null ? actor.getUser() : null)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (SalesOrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Product", itemRequest.getProductId()));

            BigDecimal unitPrice = itemRequest.getUnitPrice() != null ? itemRequest.getUnitPrice() : product.getUnitPrice();

            SalesOrderItem item = SalesOrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .build();
            order.addItem(item);

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));

            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
            adjustment.setProductId(product.getId());
            adjustment.setWarehouseId(warehouse.getId());
            adjustment.setType(MovementType.STOCK_OUT);
            adjustment.setQuantity(itemRequest.getQuantity());
            adjustment.setRemarks("Reserved for sales order");
            inventoryService.adjustStock(adjustment, actor);
        }
        order.setTotalAmount(total);
        order.setStatus(OrderStatus.COMPLETED);

        return toResponse(salesOrderRepository.save(order));
    }

    @Override
    public SalesOrderResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<SalesOrderResponse> getAll() {
        return salesOrderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public SalesOrderResponse cancel(Long id, UserPrincipal actor) {
        SalesOrder order = findEntity(id);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("Sales order is already cancelled");
        }

        for (SalesOrderItem item : order.getItems()) {
            StockAdjustmentRequest adjustment = new StockAdjustmentRequest();
            adjustment.setProductId(item.getProduct().getId());
            adjustment.setWarehouseId(order.getWarehouse().getId());
            adjustment.setType(MovementType.STOCK_IN);
            adjustment.setQuantity(item.getQuantity());
            adjustment.setRemarks("Sales order " + order.getOrderNumber() + " cancelled - stock restored");
            inventoryService.adjustStock(adjustment, actor);
        }

        order.setStatus(OrderStatus.CANCELLED);
        return toResponse(order);
    }

    private SalesOrder findEntity(Long id) {
        return salesOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("SalesOrder", id));
    }

    private String generateOrderNumber() {
        return "SO-" + Instant.now().toEpochMilli();
    }

    private SalesOrderResponse toResponse(SalesOrder order) {
        List<SalesOrderItemResponse> items = order.getItems().stream()
                .map(item -> SalesOrderItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .build())
                .toList();

        return SalesOrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerName(order.getCustomer().getName())
                .warehouseName(order.getWarehouse().getName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .items(items)
                .createdBy(order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
