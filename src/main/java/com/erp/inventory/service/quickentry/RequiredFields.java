package com.erp.inventory.service.quickentry;

import com.erp.inventory.entity.enums.QuickEntryAction;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Single source of truth for which fields each {@link QuickEntryAction} must have before it can be
 *  submitted. Used both by the frontend-facing parse pass and by the WhatsApp conversation loop. */
@Component
public class RequiredFields {

    private static final Map<QuickEntryAction, List<String>> REQUIRED = Map.of(
            QuickEntryAction.CREATE_ORDER, List.of("partyId", "productId", "orderedQty"),
            QuickEntryAction.RECORD_YARN_PURCHASE, List.of("materialId", "partyId", "quantity"),
            QuickEntryAction.ADD_FULFILLMENT_EVENT, List.of("orderId", "stage", "quantity"),
            QuickEntryAction.CREATE_PARTY, List.of("name", "roles"),
            QuickEntryAction.CREATE_PRODUCT, List.of("sku", "name", "unitPrice", "costPrice"));

    public List<String> keys(QuickEntryAction action) {
        return REQUIRED.getOrDefault(action, List.of());
    }

    /** Required keys for {@code action} that are absent, null, blank or empty in {@code fields}. */
    public List<String> missing(QuickEntryAction action, Map<String, Object> fields) {
        List<String> missing = new ArrayList<>();
        for (String key : keys(action)) {
            if (isEmpty(fields.get(key))) {
                missing.add(key);
            }
        }
        return missing;
    }

    private static boolean isEmpty(Object v) {
        return v == null
                || (v instanceof String s && s.isBlank())
                || (v instanceof List<?> l && l.isEmpty())
                || (v instanceof Map<?, ?> m && m.isEmpty());
    }
}
