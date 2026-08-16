package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.CategoryRequest;
import com.erp.inventory.dto.response.CategoryResponse;
import com.erp.inventory.entity.Category;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.CategoryRepository;
import com.erp.inventory.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Category already exists with name: " + request.getName());
        }
        Category category = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = findEntity(id);
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return toResponse(category);
    }

    @Override
    public CategoryResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findEntity(id);
        categoryRepository.delete(category);
    }

    private Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }
}
