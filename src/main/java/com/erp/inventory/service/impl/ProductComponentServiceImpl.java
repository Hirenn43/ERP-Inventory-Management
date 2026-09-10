package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.ProductComponentRequest;
import com.erp.inventory.dto.response.ProductComponentResponse;
import com.erp.inventory.entity.Material;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.ProductComponent;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.MaterialRepository;
import com.erp.inventory.repository.ProductComponentRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.service.ProductComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductComponentServiceImpl implements ProductComponentService {

    private final ProductComponentRepository productComponentRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;

    @Override
    @Transactional
    public ProductComponentResponse create(ProductComponentRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.getProductId()));
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> ResourceNotFoundException.of("Material", request.getMaterialId()));

        if (productComponentRepository.existsByProductIdAndSlotNoIgnoreCase(product.getId(), request.getSlotNo())) {
            throw new DuplicateResourceException(
                    "Slot " + request.getSlotNo() + " already has a component for this product");
        }

        ProductComponent component = ProductComponent.builder()
                .product(product)
                .slotNo(request.getSlotNo())
                .material(material)
                .quantityPerUnit(request.getQuantityPerUnit())
                .build();

        return toResponse(productComponentRepository.save(component));
    }

    @Override
    @Transactional
    public ProductComponentResponse update(Long id, ProductComponentRequest request) {
        ProductComponent component = productComponentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductComponent", id));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.getProductId()));
        Material material = materialRepository.findById(request.getMaterialId())
                .orElseThrow(() -> ResourceNotFoundException.of("Material", request.getMaterialId()));

        if (productComponentRepository.existsByProductIdAndSlotNoIgnoreCaseAndIdNot(
                product.getId(), request.getSlotNo(), id)) {
            throw new DuplicateResourceException(
                    "Slot " + request.getSlotNo() + " already has a component for this product");
        }

        component.setProduct(product);
        component.setSlotNo(request.getSlotNo());
        component.setMaterial(material);
        component.setQuantityPerUnit(request.getQuantityPerUnit());
        return toResponse(component);
    }

    @Override
    public List<ProductComponentResponse> getByProduct(Long productId) {
        return productComponentRepository.findByProductId(productId).stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductComponent component = productComponentRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductComponent", id));
        productComponentRepository.delete(component);
    }

    private ProductComponentResponse toResponse(ProductComponent component) {
        return ProductComponentResponse.builder()
                .id(component.getId())
                .productId(component.getProduct().getId())
                .productName(component.getProduct().getName())
                .slotNo(component.getSlotNo())
                .materialId(component.getMaterial().getId())
                .materialCode(component.getMaterial().getCode())
                .quantityPerUnit(component.getQuantityPerUnit())
                .build();
    }
}
