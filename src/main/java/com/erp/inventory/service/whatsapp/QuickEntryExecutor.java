package com.erp.inventory.service.whatsapp;

import com.erp.inventory.dto.request.AreaRequest;
import com.erp.inventory.dto.request.BrokerRequest;
import com.erp.inventory.dto.request.DesignRequest;
import com.erp.inventory.dto.request.JobRequest;
import com.erp.inventory.dto.request.MaterialStockMovementRequest;
import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.request.OrderRequest;
import com.erp.inventory.dto.request.PartyRequest;
import com.erp.inventory.dto.request.ProductRequest;
import com.erp.inventory.dto.request.ScheduleRequest;
import com.erp.inventory.dto.request.TransporterRequest;
import com.erp.inventory.dto.request.VehicleRequest;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.entity.WhatsappUserLink;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.QuickEntryExecException;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.AreaService;
import com.erp.inventory.service.BrokerService;
import com.erp.inventory.service.DesignService;
import com.erp.inventory.service.JobService;
import com.erp.inventory.service.MaterialStockMovementService;
import com.erp.inventory.service.OrderService;
import com.erp.inventory.service.PartyService;
import com.erp.inventory.service.ProductService;
import com.erp.inventory.service.ScheduleService;
import com.erp.inventory.service.TransporterService;
import com.erp.inventory.service.VehicleService;
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
    private final BrokerService brokerService;
    private final AreaService areaService;
    private final ScheduleService scheduleService;
    private final JobService jobService;
    private final DesignService designService;
    private final TransporterService transporterService;
    private final VehicleService vehicleService;

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
                case CREATE_BROKER -> {
                    BrokerRequest req = mapper.convertValue(fields, BrokerRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Broker created ✅ " + brokerService.create(req).getName();
                }
                case CREATE_AREA -> {
                    AreaRequest req = mapper.convertValue(fields, AreaRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Area created ✅ " + areaService.create(req).getName();
                }
                case CREATE_SCHEDULE -> {
                    ScheduleRequest req = mapper.convertValue(fields, ScheduleRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Schedule created ✅ " + scheduleService.create(req).getName();
                }
                case CREATE_JOB -> {
                    JobRequest req = mapper.convertValue(fields, JobRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Job created ✅ " + jobService.create(req).getName();
                }
                case CREATE_DESIGN -> {
                    DesignRequest req = mapper.convertValue(fields, DesignRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Design created ✅ " + designService.create(req).getName();
                }
                case CREATE_TRANSPORTER -> {
                    TransporterRequest req = mapper.convertValue(fields, TransporterRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Transporter created ✅ " + transporterService.create(req).getName();
                }
                case CREATE_VEHICLE -> {
                    VehicleRequest req = mapper.convertValue(fields, VehicleRequest.class);
                    scope(link, req::setCompanyId);
                    yield "Vehicle created ✅ " + vehicleService.create(req).getName();
                }
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

    /** Apply the WhatsApp user's company to a *Request, matching the "New" flows. */
    private static void scope(WhatsappUserLink link, java.util.function.Consumer<Long> setter) {
        if (link.getCompany() != null) {
            setter.accept(link.getCompany().getId());
        }
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
