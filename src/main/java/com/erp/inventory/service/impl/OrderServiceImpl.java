package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.request.OrderRequest;
import com.erp.inventory.dto.response.OrderFulfillmentEventResponse;
import com.erp.inventory.dto.response.OrderMaterialRequirementResponse;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.entity.Order;
import com.erp.inventory.entity.OrderFulfillmentEvent;
import com.erp.inventory.entity.Party;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.ProductComponent;
import com.erp.inventory.entity.Resource;
import com.erp.inventory.entity.enums.FulfillmentStage;
import com.erp.inventory.exception.ResourceNotFoundException;
import com.erp.inventory.repository.OrderRepository;
import com.erp.inventory.repository.PartyRepository;
import com.erp.inventory.repository.ProductComponentRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.ResourceRepository;
import com.erp.inventory.security.UserPrincipal;
import com.erp.inventory.service.MaterialStockMovementService;
import com.erp.inventory.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final PartyRepository partyRepository;
    private final ProductRepository productRepository;
    private final ProductComponentRepository productComponentRepository;
    private final ResourceRepository resourceRepository;
    private final MaterialStockMovementService materialStockMovementService;

    @Override
    @Transactional
    public OrderResponse create(OrderRequest request, UserPrincipal actor) {
        Party party = partyRepository.findOneById(request.getPartyId())
                .orElseThrow(() -> ResourceNotFoundException.of("Party", request.getPartyId()));
        Product product = productRepository.findOneById(request.getProductId())
                .orElseThrow(() -> ResourceNotFoundException.of("Product", request.getProductId()));
        ProductComponent component = resolveComponent(request.getComponentId());

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .party(party)
                .product(product)
                .component(component)
                .date(request.getDate())
                .rate(request.getRate())
                .orderedQty(request.getOrderedQty())
                .variant(request.getVariant() != null ? request.getVariant() : new HashMap<>())
                .createdBy(actor != null ? actor.getUser() : null)
                .build();

        OrderResponse response = toResponse(orderRepository.save(order));
        response.setWarnings(shortfallWarnings(response.getMaterialRequirement()));
        return response;
    }

    private List<String> shortfallWarnings(OrderMaterialRequirementResponse requirement) {
        List<String> warnings = new ArrayList<>();
        if (requirement == null || requirement.getLines() == null) {
            return warnings;
        }
        for (OrderMaterialRequirementResponse.Line line : requirement.getLines()) {
            if (line.isQuantityPerUnitMissing()) {
                warnings.add("Yarn " + line.getMaterialCode()
                        + " has no per-unit quantity on the design BOM - usage could not be checked");
            } else if (!line.isSufficient()) {
                warnings.add("Yarn " + line.getMaterialCode() + " short by " + strip(line.getShortfallQty())
                        + " (need " + strip(line.getRequiredQty()) + ", have " + strip(line.getAvailableQty()) + ")");
            }
        }
        return warnings;
    }

    private static String strip(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    @Override
    public OrderResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional
    public OrderResponse addFulfillmentEvent(Long orderId, OrderFulfillmentEventRequest request) {
        Order order = findEntity(orderId);
        Resource resource = null;
        if (request.getResourceId() != null) {
            resource = resourceRepository.findOneById(request.getResourceId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Resource", request.getResourceId()));
        }

        validateFulfillmentEvent(order, request.getStage(), request.getQuantity());

        OrderFulfillmentEvent event = OrderFulfillmentEvent.builder()
                .stage(request.getStage())
                .quantity(request.getQuantity())
                .date(request.getDate())
                .resource(resource)
                .remarks(request.getRemarks())
                .build();
        order.addFulfillmentEvent(event);

        return toResponse(order);
    }

    private void validateFulfillmentEvent(Order order, FulfillmentStage stage, BigDecimal quantity) {
        BigDecimal ordered = order.getOrderedQty();
        BigDecimal completed = sumByStage(order, FulfillmentStage.COMPLETED);
        BigDecimal dispatched = sumByStage(order, FulfillmentStage.DISPATCHED);
        BigDecimal settled = sumByStage(order, FulfillmentStage.SETTLED);
        BigDecimal returned = sumByStage(order, FulfillmentStage.RETURNED);

        BigDecimal ceiling;
        String ceilingLabel;
        switch (stage) {
            case COMPLETED -> { ceiling = ordered.subtract(completed); ceilingLabel = "remaining ordered quantity"; }
            case DISPATCHED -> { ceiling = completed.subtract(dispatched); ceilingLabel = "quantity completed but not yet dispatched"; }
            case SETTLED -> { ceiling = dispatched.subtract(settled); ceilingLabel = "quantity dispatched but not yet settled"; }
            case RETURNED -> { ceiling = dispatched.subtract(returned); ceilingLabel = "quantity dispatched but not yet returned"; }
            default -> { return; }
        }
        if (quantity.compareTo(ceiling) > 0) {
            throw new IllegalArgumentException(stage + " quantity " + strip(quantity)
                    + " exceeds the " + ceilingLabel + " (" + strip(ceiling.max(BigDecimal.ZERO)) + ")");
        }
    }

    @Override
    public OrderMaterialRequirementResponse computeRequirement(Long productId, Long componentId, BigDecimal qty) {
        Product product = productRepository.findOneById(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        resolveComponent(componentId); // validate if supplied
        BigDecimal quantity = qty != null ? qty : BigDecimal.ZERO;
        return buildRequirement(product.getId(), quantity,
                productComponentRepository.findByProductId(product.getId()));
    }

    private OrderMaterialRequirementResponse buildRequirement(Long productId, BigDecimal orderedQty,
                                                              List<ProductComponent> bom) {
        Set<Long> materialIds = new LinkedHashSet<>();
        for (ProductComponent pc : bom) {
            materialIds.add(pc.getMaterial().getId());
        }
        Map<Long, BigDecimal> onHand = materialStockMovementService.availableQuantities(materialIds);

        List<OrderMaterialRequirementResponse.Line> lines = new ArrayList<>();
        boolean anyShortfall = false;
        for (ProductComponent pc : bom) {
            BigDecimal available = onHand.getOrDefault(pc.getMaterial().getId(), BigDecimal.ZERO);
            BigDecimal perUnit = pc.getQuantityPerUnit();
            boolean missing = perUnit == null;
            BigDecimal required = missing ? null : perUnit.multiply(orderedQty);
            BigDecimal shortfall = missing ? BigDecimal.ZERO : required.subtract(available).max(BigDecimal.ZERO);
            boolean sufficient = missing || shortfall.signum() == 0;
            if (!sufficient) {
                anyShortfall = true;
            }
            lines.add(OrderMaterialRequirementResponse.Line.builder()
                    .slotNo(pc.getSlotNo())
                    .materialId(pc.getMaterial().getId())
                    .materialCode(pc.getMaterial().getCode())
                    .unit(pc.getMaterial().getUnit())
                    .quantityPerUnit(perUnit)
                    .requiredQty(required)
                    .availableQty(available)
                    .shortfallQty(shortfall)
                    .sufficient(sufficient)
                    .quantityPerUnitMissing(missing)
                    .build());
        }
        return OrderMaterialRequirementResponse.builder()
                .productId(productId)
                .orderedQty(orderedQty)
                .lines(lines)
                .anyShortfall(anyShortfall)
                .build();
    }

    private ProductComponent resolveComponent(Long componentId) {
        if (componentId == null) {
            return null;
        }
        return productComponentRepository.findOneById(componentId)
                .orElseThrow(() -> ResourceNotFoundException.of("ProductComponent", componentId));
    }

    private Order findEntity(Long id) {
        return orderRepository.findOneById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Order", id));
    }

    private String generateOrderNumber() {
        // epoch millis alone collide when two orders are created in the same millisecond (scripted /
        // bulk creation). Add a random suffix and check the unique index before returning; the DB
        // constraint is still the final backstop for the rare same-suffix race.
        for (int attempt = 0; attempt < 10; attempt++) {
            String candidate = "ORD-" + Instant.now().toEpochMilli() + "-"
                    + ThreadLocalRandom.current().nextInt(1000, 10000);
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not allocate a unique order number after 10 attempts");
    }

    private OrderResponse toResponse(Order order) {
        List<OrderFulfillmentEventResponse> events = order.getFulfillmentEvents().stream()
                .map(this::toEventResponse)
                .toList();

        BigDecimal ordered = order.getOrderedQty();
        BigDecimal completed = sumByStage(order, FulfillmentStage.COMPLETED);
        BigDecimal dispatched = sumByStage(order, FulfillmentStage.DISPATCHED);
        BigDecimal settled = sumByStage(order, FulfillmentStage.SETTLED);
        BigDecimal returned = sumByStage(order, FulfillmentStage.RETURNED);
        boolean isCompleted = dispatched.compareTo(ordered) >= 0 && settled.compareTo(ordered) >= 0;

        OrderMaterialRequirementResponse requirement = buildRequirement(
                order.getProduct().getId(), ordered,
                productComponentRepository.findByProductId(order.getProduct().getId()));

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .partyName(order.getParty().getName())
                .productName(order.getProduct().getName())
                .componentId(order.getComponent() != null ? order.getComponent().getId() : null)
                .date(order.getDate())
                .rate(order.getRate())
                .orderedQty(order.getOrderedQty())
                .variant(order.getVariant())
                .completedQty(completed)
                .dispatchedQty(dispatched)
                .settledQty(settled)
                .returnedQty(returned)
                .pendingQty(ordered.subtract(completed).max(BigDecimal.ZERO))
                .status(isCompleted ? "COMPLETED" : "PENDING")
                .materialRequirement(requirement)
                .materialShortfall(requirement.isAnyShortfall())
                .fulfillmentEvents(events)
                .createdBy(order.getCreatedBy() != null ? order.getCreatedBy().getUsername() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }

    private BigDecimal sumByStage(Order order, FulfillmentStage stage) {
        return order.getFulfillmentEvents().stream()
                .filter(event -> event.getStage() == stage)
                .map(OrderFulfillmentEvent::getQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderFulfillmentEventResponse toEventResponse(OrderFulfillmentEvent event) {
        return OrderFulfillmentEventResponse.builder()
                .id(event.getId())
                .stage(event.getStage())
                .quantity(event.getQuantity())
                .date(event.getDate())
                .resourceName(event.getResource() != null ? event.getResource().getName() : null)
                .remarks(event.getRemarks())
                .build();
    }
}
