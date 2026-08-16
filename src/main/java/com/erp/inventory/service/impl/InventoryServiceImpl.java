package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.StockAdjustmentRequest;
import com.erp.inventory.dto.response.InventoryResponse;
import com.erp.inventory.dto.response.StockMovementResponse;
import com.erp.inventory.entity.Inventory;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.StockMovement;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.Warehouse;
import com.erp.inventory.entity.enums.MovementType;
import com.erp.inventory.exception.InsufficientStockException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.InventoryRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.StockMovementRepository;
import com.erp.inventory.repository.WarehouseRepository;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;

    @Override
    public List<InventoryResponse> getAll() {
        return inventoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<InventoryResponse> getByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<InventoryResponse> getByWarehouse(Long warehouseId) {
        return inventoryRepository.findByWarehouseId(warehouseId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public StockMovementResponse adjustStock(StockAdjustmentRequest request, UserPrincipal actor) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> ResourceNotFoundException.of("Warehouse", request.getWarehouseId()));

        Inventory inventory = inventoryRepository
                .findByProductIdAndWarehouseId(product.getId(), warehouse.getId())
                .orElseGet(() -> Inventory.builder().product(product).warehouse(warehouse).quantity(0).build());

        applyMovement(inventory, request.getType(), request.getQuantity());
        inventoryRepository.save(inventory);

        StockMovement movement = StockMovement.builder()
                .product(product)
                .warehouse(warehouse)
                .type(request.getType())
                .quantity(request.getQuantity())
                .remarks(request.getRemarks())
                .performedBy(actor != null ? actor.getUser() : null)
                .build();

        return toResponse(stockMovementRepository.save(movement));
    }

    @Override
    public List<StockMovementResponse> getMovementsForProduct(Long productId) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void applyMovement(Inventory inventory, MovementType type, Integer quantity) {
        switch (type) {
            case STOCK_IN, ADJUSTMENT -> inventory.setQuantity(inventory.getQuantity() + quantity);
            case STOCK_OUT, TRANSFER -> {
                if (inventory.getQuantity() < quantity) {
                    throw new InsufficientStockException(
                            "Insufficient stock for product id " + inventory.getProduct().getId()
                                    + " in warehouse id " + inventory.getWarehouse().getId()
                                    + ". Available: " + inventory.getQuantity() + ", requested: " + quantity);
                }
                inventory.setQuantity(inventory.getQuantity() - quantity);
            }
        }
    }

    private InventoryResponse toResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProduct().getId())
                .productName(inventory.getProduct().getName())
                .sku(inventory.getProduct().getSku())
                .warehouseId(inventory.getWarehouse().getId())
                .warehouseName(inventory.getWarehouse().getName())
                .quantity(inventory.getQuantity())
                .build();
    }

    private StockMovementResponse toResponse(StockMovement movement) {
        User performedBy = movement.getPerformedBy();
        return StockMovementResponse.builder()
                .id(movement.getId())
                .productName(movement.getProduct().getName())
                .warehouseName(movement.getWarehouse().getName())
                .type(movement.getType())
                .quantity(movement.getQuantity())
                .remarks(movement.getRemarks())
                .performedBy(performedBy != null ? performedBy.getUsername() : null)
                .createdAt(movement.getCreatedAt())
                .build();
    }
}
