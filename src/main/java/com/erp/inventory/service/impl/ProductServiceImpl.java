package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.ProductRequest;
import com.erp.inventory.dto.response.ProductResponse;
import com.erp.inventory.entity.Category;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.Supplier;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CategoryRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.SupplierRepository;
import com.erp.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product already exists with SKU: " + request.getSku());
        }

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .unitPrice(request.getUnitPrice())
                .costPrice(request.getCostPrice())
                .reorderLevel(request.getReorderLevel() != null ? request.getReorderLevel() : 10)
                .category(resolveCategory(request.getCategoryId()))
                .supplier(resolveSupplier(request.getSupplierId()))
                .active(true)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findEntity(id);
        product.setSku(request.getSku());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setUnitPrice(request.getUnitPrice());
        product.setCostPrice(request.getCostPrice());
        if (request.getReorderLevel() != null) {
            product.setReorderLevel(request.getReorderLevel());
        }
        product.setCategory(resolveCategory(request.getCategoryId()));
        product.setSupplier(resolveSupplier(request.getSupplierId()));
        return toResponse(product);
    }

    @Override
    public ProductResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findEntity(id);
        product.setActive(false);
        productRepository.save(product);
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", supplierId));
    }

    private Product findEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .unitPrice(product.getUnitPrice())
                .costPrice(product.getCostPrice())
                .reorderLevel(product.getReorderLevel())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .supplierName(product.getSupplier() != null ? product.getSupplier().getName() : null)
                .active(product.isActive())
                .build();
    }
}
