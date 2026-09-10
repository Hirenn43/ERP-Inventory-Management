package com.erp.inventory.service.impl;

import com.erp.inventory.dto.response.PageResponse;
import com.erp.inventory.repository.spec.TextSearchSpec;
import org.springframework.data.domain.Pageable;
import com.erp.inventory.dto.request.ProductRequest;
import com.erp.inventory.dto.response.ProductResponse;
import com.erp.inventory.entity.Category;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Product;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CategoryRepository;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.security.TenantContext;
import com.erp.inventory.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CompanyRepository companyRepository;

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
                .company(resolveCompany(request.getCompanyId()))
                .active(true)
                .attributes(request.getAttributes() != null ? request.getAttributes() : new HashMap<>())
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
        product.setCompany(resolveCompany(request.getCompanyId()));
        if (request.getAttributes() != null) {
            product.setAttributes(request.getAttributes());
        }
        return toResponse(product);
    }

    @Override
    public ProductResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<ProductResponse> getAll() {
        // Soft-deleted (active = false) products are hidden from list views.
        return productRepository.findByActiveTrue().stream().map(this::toResponse).toList();
    }

    @Override
    public PageResponse<ProductResponse> getPage(Pageable pageable, String q) {
        return PageResponse.of(productRepository.findAll(TextSearchSpec.contains(q, "sku", "name"), pageable).map(this::toResponse));
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

    private Company resolveCompany(Long ignoredFromRequest) {
        // Tenant is derived from the authenticated caller, never from the request body.
        Long tenantId = TenantContext.get();
        return tenantId == null ? null : companyRepository.getReferenceById(tenantId);
    }

    private Product findEntity(Long id) {
        return productRepository.findOneById(id)
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
                .companyId(product.getCompany() != null ? product.getCompany().getId() : null)
                .active(product.isActive())
                .attributes(product.getAttributes())
                .build();
    }
}
