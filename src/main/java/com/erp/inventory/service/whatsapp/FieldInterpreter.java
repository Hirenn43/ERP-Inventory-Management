package com.erp.inventory.service.whatsapp;

import com.erp.inventory.entity.enums.FulfillmentStage;
import com.erp.inventory.entity.enums.PartyRole;
import com.erp.inventory.service.quickentry.EntityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Interprets one free-text WhatsApp reply as the value for a specific required field. */
@Component
@RequiredArgsConstructor
public class FieldInterpreter {

    private final EntityResolver resolver;

    private static final Map<String, EntityResolver.Kind> ID_FIELDS = Map.of(
            "partyId", EntityResolver.Kind.PARTY,
            "productId", EntityResolver.Kind.PRODUCT,
            "materialId", EntityResolver.Kind.MATERIAL,
            "orderId", EntityResolver.Kind.ORDER,
            "resourceId", EntityResolver.Kind.RESOURCE);

    private static final Set<String> POSITIVE_NUMBER_FIELDS = Set.of("orderedQty", "quantity");
    private static final Set<String> NUMBER_FIELDS = Set.of(
            "orderedQty", "quantity", "rate", "price", "unitPrice", "costPrice");

    /** Kinds of result the caller (orchestrator) must handle. */
    public enum Kind { FILLED, MENU, FAILED }

    public record Outcome(Kind kind, Object value, String field,
                          List<EntityResolver.Match> options, String message) {

        static Outcome filled(Object value) {
            return new Outcome(Kind.FILLED, value, null, null, null);
        }

        static Outcome menu(String field, List<EntityResolver.Match> options) {
            return new Outcome(Kind.MENU, null, field, options, null);
        }

        static Outcome failed(String message) {
            return new Outcome(Kind.FAILED, null, null, null, message);
        }
    }

    public Outcome interpret(String field, String text) {
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.isEmpty()) {
            return Outcome.failed("Please type a value.");
        }
        if (ID_FIELDS.containsKey(field)) {
            return interpretId(field, trimmed);
        }
        if (NUMBER_FIELDS.contains(field)) {
            return interpretNumber(field, trimmed);
        }
        if ("stage".equals(field)) {
            return interpretStage(trimmed);
        }
        if ("roles".equals(field)) {
            return interpretRoles(trimmed);
        }
        return Outcome.filled(trimmed);
    }

    private Outcome interpretId(String field, String text) {
        EntityResolver.Kind kind = ID_FIELDS.get(field);
        List<EntityResolver.Match> matches = resolver.candidates(text, kind, 5);
        if (matches.isEmpty()) {
            return Outcome.failed("No " + kind.name().toLowerCase() + " matches \"" + text
                    + "\". Reply the exact name, or 'cancel'.");
        }
        if (matches.size() == 1) {
            return Outcome.filled(matches.get(0).id());
        }
        return Outcome.menu(field, matches);
    }

    private Outcome interpretNumber(String field, String text) {
        String cleaned = text.replaceAll("[^0-9.]", "");
        if (cleaned.isEmpty() || cleaned.equals(".")) {
            return Outcome.failed("Enter a number, e.g. 1250.");
        }
        try {
            BigDecimal value = new BigDecimal(cleaned);
            if (POSITIVE_NUMBER_FIELDS.contains(field) && value.signum() <= 0) {
                return Outcome.failed("Quantity must be greater than zero.");
            }
            if (value.signum() < 0) {
                return Outcome.failed("Value cannot be negative.");
            }
            return Outcome.filled(value);
        } catch (NumberFormatException e) {
            return Outcome.failed("Enter a number, e.g. 1250.");
        }
    }

    private Outcome interpretStage(String text) {
        String upper = text.trim().toUpperCase();
        for (FulfillmentStage stage : FulfillmentStage.values()) {
            if (stage.name().equals(upper) || stage.name().startsWith(upper) || upper.startsWith(stage.name())) {
                return Outcome.filled(stage.name());
            }
        }
        // keyword forms: "complete", "dispatch", "settle", "return"
        String lower = text.toLowerCase();
        if (lower.startsWith("complet")) return Outcome.filled(FulfillmentStage.COMPLETED.name());
        if (lower.startsWith("dispatch")) return Outcome.filled(FulfillmentStage.DISPATCHED.name());
        if (lower.startsWith("settl")) return Outcome.filled(FulfillmentStage.SETTLED.name());
        if (lower.startsWith("return")) return Outcome.filled(FulfillmentStage.RETURNED.name());
        return Outcome.failed("Stage must be one of: completed, dispatched, settled, returned.");
    }

    private Outcome interpretRoles(String text) {
        List<String> roles = new ArrayList<>();
        for (String token : text.toLowerCase().split("[,/\\s]+")) {
            switch (token) {
                case "customer", "buyer" -> add(roles, PartyRole.CUSTOMER);
                case "supplier", "vendor" -> add(roles, PartyRole.SUPPLIER);
                case "manufacturer", "maker" -> add(roles, PartyRole.MANUFACTURER);
                case "jobworker", "job", "worker", "job-worker" -> add(roles, PartyRole.JOB_WORKER);
                default -> { /* ignore unknown token */ }
            }
        }
        if (roles.isEmpty()) {
            return Outcome.failed("Type must be one of: customer, supplier, manufacturer, job worker.");
        }
        return Outcome.filled(roles);
    }

    private static void add(List<String> roles, PartyRole role) {
        if (!roles.contains(role.name())) {
            roles.add(role.name());
        }
    }
}
