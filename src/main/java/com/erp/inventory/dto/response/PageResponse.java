package com.erp.inventory.dto.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Stable pagination envelope for list endpoints. Spring Data's own {@code Page} JSON shape is
 * unstable across versions, so every paged endpoint returns this instead.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
