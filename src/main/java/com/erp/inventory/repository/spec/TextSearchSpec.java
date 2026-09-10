package com.erp.inventory.repository.spec;

import jakarta.persistence.criteria.Predicate;
import java.util.Arrays;
import org.springframework.data.jpa.domain.Specification;

/** Case-insensitive "contains" search across one or more string columns of an entity. */
public final class TextSearchSpec {

    private TextSearchSpec() {
    }

    /** Returns {@code null} (match-all) when {@code q} is blank, so callers can pass it straight through. */
    public static <T> Specification<T> contains(String q, String... fields) {
        if (q == null || q.isBlank() || fields.length == 0) {
            return null;
        }
        String like = "%" + q.trim().toLowerCase() + "%";
        return (root, query, cb) -> {
            Predicate[] likes = Arrays.stream(fields)
                    .map(f -> cb.like(cb.lower(root.get(f).as(String.class)), like))
                    .toArray(Predicate[]::new);
            return cb.or(likes);
        };
    }
}
