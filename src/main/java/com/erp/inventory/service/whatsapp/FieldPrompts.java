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

    private static final Map<QuickEntryAction, Map<String, String>> OVERRIDES = Map.ofEntries(
            Map.entry(QuickEntryAction.RECORD_YARN_PURCHASE, Map.of(
                    "partyId", "Supplier name?",
                    "quantity", "Quantity (kg)?")),
            Map.entry(QuickEntryAction.CREATE_ORDER, Map.of(
                    "partyId", "Customer name?")),
            Map.entry(QuickEntryAction.ADD_FULFILLMENT_EVENT, Map.of(
                    "quantity", "Quantity (meters)?")),
            Map.entry(QuickEntryAction.CREATE_PARTY, Map.of(
                    "name", "Party name?")),
            Map.entry(QuickEntryAction.CREATE_PRODUCT, Map.of(
                    "name", "Design name?")),
            Map.entry(QuickEntryAction.CREATE_BROKER, Map.of("name", "Broker name?")),
            Map.entry(QuickEntryAction.CREATE_AREA, Map.of("name", "Area name?")),
            Map.entry(QuickEntryAction.CREATE_SCHEDULE, Map.of("name", "Schedule name?")),
            Map.entry(QuickEntryAction.CREATE_JOB, Map.of("name", "Job name?")),
            Map.entry(QuickEntryAction.CREATE_DESIGN, Map.of("name", "Design / pattern name?")),
            Map.entry(QuickEntryAction.CREATE_TRANSPORTER, Map.of("name", "Transporter name?")),
            Map.entry(QuickEntryAction.CREATE_VEHICLE, Map.of("name", "Vehicle number?")));

    public String prompt(QuickEntryAction action, String field) {
        Map<String, String> overrides = OVERRIDES.get(action);
        if (overrides != null && overrides.containsKey(field)) {
            return overrides.get(field);
        }
        return GENERIC.getOrDefault(field, field + "?");
    }
}
