package com.erp.inventory.service.whatsapp;

import com.erp.inventory.dto.request.MaterialStockMovementRequest;
import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.request.OrderRequest;
import com.erp.inventory.dto.request.PartyRequest;
import com.erp.inventory.dto.request.ProductRequest;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.entity.WhatsappUserLink;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.QuickEntryExecException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.MaterialStockMovementService;
import com.erp.inventory.service.OrderService;
import com.erp.inventory.service.PartyService;
import com.erp.inventory.service.ProductService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Turns a completed quick-entry {@code fields} map into an actual create call. Any failure is
 *  rethrown as {@link QuickEntryExecException} with a WhatsApp-ready message. */
@Component
@RequiredArgsConstructor
public class QuickEntryExecutor {

    private final OrderService orderService;
    private final MaterialStockMovementService materialStockMovementService;
    private final PartyService partyService;
    private final ProductService productService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** @return a short confirmation line to send back to the user. */
    public String execute(QuickEntryAction action, Map<String, Object> rawFields, WhatsappUserLink link) {
        Map<String, Object> fields = new LinkedHashMap<>(rawFields);
        try {
            return switch (action) {
                case CREATE_ORDER -> createOrder(fields, link);
                case RECORD_YARN_PURCHASE -> recordPurchase(fields);
                case ADD_FULFILLMENT_EVENT -> addFulfillmentEvent(fields);
                case CREATE_PARTY -> createParty(fields, link);
                case CREATE_PRODUCT -> createProduct(fields, link);
                case UNKNOWN -> throw new QuickEntryExecException("Sorry, I couldn't tell what to record.");
            };
        } catch (ResourceNotFoundException | DuplicateResourceException | IllegalArgumentException
                 | ConstraintViolationException e) {
            throw new QuickEntryExecException("⚠️ " + e.getMessage(), e);
        }
    }

    private String createOrder(Map<String, Object> fields, WhatsappUserLink link) {
        defaultDate(fields);
        OrderRequest req = mapper.convertValue(fields, OrderRequest.class);
        OrderResponse order = orderService.create(req, new UserPrincipal(link.getUser()));
        return "Entry completed ✅ " + order.getOrderNumber();
    }

    private String recordPurchase(Map<String, Object> fields) {
        fields.put("direction", "IN");
        defaultDate(fields);
        MaterialStockMovementRequest req = mapper.convertValue(fields, MaterialStockMovementRequest.class);
        var movement = materialStockMovementService.create(req);
        return "Purchase recorded ✅ (movement #" + movement.getId() + ")";
    }

    private String addFulfillmentEvent(Map<String, Object> fields) {
        Object orderId = fields.remove("orderId");
        if (!(orderId instanceof Number n)) {
            throw new QuickEntryExecException("Couldn't identify the order.");
        }
        defaultDate(fields);
        OrderFulfillmentEventRequest req = mapper.convertValue(fields, OrderFulfillmentEventRequest.class);
        OrderResponse order = orderService.addFulfillmentEvent(n.longValue(), req);
        return "Updated ✅ " + order.getOrderNumber() + " — " + req.getStage()
                + " " + strip(req.getQuantity());
    }

    private String createParty(Map<String, Object> fields, WhatsappUserLink link) {
        PartyRequest req = mapper.convertValue(fields, PartyRequest.class);
        if (link.getCompany() != null) {
            req.setCompanyId(link.getCompany().getId());
        }
        var party = partyService.create(req);
        return "Party created ✅ " + party.getName() + " (#" + party.getId() + ")";
    }

    private String createProduct(Map<String, Object> fields, WhatsappUserLink link) {
        ProductRequest req = mapper.convertValue(fields, ProductRequest.class);
        if (link.getCompany() != null) {
            req.setCompanyId(link.getCompany().getId());
        }
        var product = productService.create(req);
        return "Design created ✅ " + product.getSku() + " — " + product.getName();
    }

    private static void defaultDate(Map<String, Object> fields) {
        Object d = fields.get("date");
        if (d == null || (d instanceof String s && s.isBlank())) {
            fields.put("date", LocalDate.now().toString());
        }
    }

    private static String strip(BigDecimal v) {
        return v == null ? "" : v.stripTrailingZeros().toPlainString();
    }
}
