package com.erp.inventory.service.whatsapp;

import com.erp.inventory.entity.enums.QuickEntryAction;
import org.springframework.stereotype.Component;

import java.util.Map;

/** The question the bot asks for each required field, with a few per-action overrides where the
 *  generic wording would read oddly. */
@Component
public class FieldPrompts {

    private static final Map<String, String> GENERIC = Map.ofEntries(
            Map.entry("partyId", "Party name?"),
            Map.entry("productId", "Design / SKU?"),
            Map.entry("materialId", "Yarn / material?"),
            Map.entry("orderId", "Order number?"),
            Map.entry("resourceId", "Loom / resource?"),
            Map.entry("orderedQty", "Quantity (meters)?"),
            Map.entry("quantity", "Quantity?"),
            Map.entry("rate", "Rate?"),
            Map.entry("price", "Price?"),
            Map.entry("unitPrice", "Selling price?"),
            Map.entry("costPrice", "Cost price?"),
            Map.entry("stage", "Stage? (completed / dispatched / settled / returned)"),
            Map.entry("roles", "Type? (customer / supplier / manufacturer / job worker)"),
            Map.entry("sku", "SKU code?"),
            Map.entry("name", "Name?"),
            Map.entry("date", "Date? (YYYY-MM-DD, or 'today')"));

    private static final Map<QuickEntryAction, Map<String, String>> OVERRIDES = Map.of(
            QuickEntryAction.RECORD_YARN_PURCHASE, Map.of(
                    "partyId", "Supplier name?",
                    "quantity", "Quantity (kg)?"),
            QuickEntryAction.CREATE_ORDER, Map.of(
                    "partyId", "Customer name?"),
            QuickEntryAction.ADD_FULFILLMENT_EVENT, Map.of(
                    "quantity", "Quantity (meters)?"),
            QuickEntryAction.CREATE_PARTY, Map.of(
                    "name", "Party name?"),
            QuickEntryAction.CREATE_PRODUCT, Map.of(
                    "name", "Design name?"));

    public String prompt(QuickEntryAction action, String field) {
        Map<String, String> overrides = OVERRIDES.get(action);
        if (overrides != null && overrides.containsKey(field)) {
            return overrides.get(field);
        }
        return GENERIC.getOrDefault(field, field + "?");
    }
}
