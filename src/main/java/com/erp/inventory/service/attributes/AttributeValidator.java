package com.erp.inventory.service.attributes;

import com.erp.inventory.entity.CustomFieldDefinition;
import com.erp.inventory.entity.enums.FieldDataType;
import com.erp.inventory.exception.AttributeValidationException;
import com.erp.inventory.repository.CustomFieldDefinitionRepository;
import com.erp.inventory.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Validates a master's {@code attributes} JSONB bag against the tenant's
 * {@link CustomFieldDefinition}s before it is persisted.
 *
 * <p>Opt-in: if the tenant has defined no fields for the resource, the incoming map is
 * returned untouched and behaviour is exactly as before this feature existed. Keys with no
 * matching definition are passed through (migration-safe).
 */
@Component
@RequiredArgsConstructor
public class AttributeValidator {

    /** resource key -> table name, for the uniqueness check. Only entities with an {@code attributes} column. */
    private static final Map<String, String> RESOURCE_TABLES = Map.ofEntries(
            Map.entry("parties", "parties"),
            Map.entry("materials", "materials"),
            Map.entry("products", "products"),
            Map.entry("brokers", "brokers"),
            Map.entry("areas", "areas"),
            Map.entry("schedules", "schedules"),
            Map.entry("jobs", "jobs"),
            Map.entry("designs", "designs"),
            Map.entry("transporters", "transporters"),
            Map.entry("vehicles", "vehicles"),
            Map.entry("resources", "resources"),
            Map.entry("warehouses", "warehouses"),
            Map.entry("companies", "companies"),
            Map.entry("categories", "categories"),
            Map.entry("materialMovements", "material_stock_movements"));

    private final CustomFieldDefinitionRepository definitionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * @param resource        the master resource key (e.g. "parties")
     * @param incoming         the attributes map from the request (may be null)
     * @param currentEntityId  the id of the row being updated, or null on create (for uniqueness)
     * @return the coerced attributes map to persist
     */
    public Map<String, Object> validate(String resource, Map<String, Object> incoming, Long currentEntityId) {
        Map<String, Object> attrs = incoming != null ? new LinkedHashMap<>(incoming) : new LinkedHashMap<>();

        List<CustomFieldDefinition> defs =
                definitionRepository.findByResourceAndActiveTrueOrderByDisplayOrderAscIdAsc(resource);
        if (defs.isEmpty()) {
            return attrs; // opt-in — nothing configured, nothing to enforce
        }

        List<String> errors = new ArrayList<>();
        for (CustomFieldDefinition def : defs) {
            Object raw = attrs.get(def.getFieldKey());
            boolean blank = raw == null || (raw instanceof String s && s.isBlank());

            if (blank) {
                if (def.isRequired()) {
                    errors.add(def.getLabel() + " is required");
                }
                attrs.remove(def.getFieldKey());
                continue;
            }

            Object coerced;
            try {
                coerced = coerce(def, raw);
            } catch (IllegalArgumentException ex) {
                errors.add(def.getLabel() + ": " + ex.getMessage());
                continue;
            }
            attrs.put(def.getFieldKey(), coerced);

            switch (def.getDataType()) {
                case TEXT, SELECT -> {
                    if (def.getMaxLength() != null && coerced.toString().length() > def.getMaxLength()) {
                        errors.add(def.getLabel() + " must be at most " + def.getMaxLength() + " characters");
                    }
                    if (def.getDataType() == FieldDataType.SELECT
                            && !def.getOptions().contains(coerced.toString())) {
                        errors.add(def.getLabel() + " must be one of: " + String.join(", ", def.getOptions()));
                    }
                }
                case NUMBER, INTEGER -> {
                    BigDecimal n = (BigDecimal) coerced;
                    if (def.getMinValue() != null && n.compareTo(def.getMinValue()) < 0) {
                        errors.add(def.getLabel() + " must be at least " + def.getMinValue());
                    }
                    if (def.getMaxValue() != null && n.compareTo(def.getMaxValue()) > 0) {
                        errors.add(def.getLabel() + " must be at most " + def.getMaxValue());
                    }
                }
                default -> { }
            }

            if (def.isUniqueValue() && !valueIsUnique(resource, def.getFieldKey(), coerced.toString(), currentEntityId)) {
                errors.add(def.getLabel() + " '" + coerced + "' is already used by another record");
            }
        }

        if (!errors.isEmpty()) {
            throw new AttributeValidationException(errors);
        }
        return attrs;
    }

    private Object coerce(CustomFieldDefinition def, Object raw) {
        String s = raw.toString().trim();
        return switch (def.getDataType()) {
            case TEXT, SELECT -> s;
            case BOOLEAN -> {
                if (raw instanceof Boolean b) {
                    yield b;
                }
                if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("false")) {
                    yield Boolean.parseBoolean(s);
                }
                throw new IllegalArgumentException("must be true or false");
            }
            case INTEGER -> {
                try {
                    BigDecimal n = new BigDecimal(s);
                    if (n.stripTrailingZeros().scale() > 0) {
                        throw new IllegalArgumentException("must be a whole number");
                    }
                    yield n;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("must be a whole number");
                }
            }
            case NUMBER -> {
                try {
                    yield new BigDecimal(s);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("must be a number");
                }
            }
            case DATE -> {
                try {
                    yield LocalDate.parse(s).toString();
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("must be a date (YYYY-MM-DD)");
                }
            }
        };
    }

    private boolean valueIsUnique(String resource, String key, String value, Long currentEntityId) {
        String table = RESOURCE_TABLES.get(resource);
        if (table == null) {
            return true; // uniqueness not supported for this resource
        }
        Long companyId = TenantContext.get();
        Query query = entityManager.createNativeQuery(
                "SELECT count(*) FROM " + table
                        + " WHERE company_id = :companyId AND attributes ->> :key = :value AND id <> :id");
        query.setParameter("companyId", companyId);
        query.setParameter("key", key);
        query.setParameter("value", value);
        query.setParameter("id", currentEntityId != null ? currentEntityId : -1L);
        Number count = (Number) query.getSingleResult();
        return count.longValue() == 0;
    }
}
