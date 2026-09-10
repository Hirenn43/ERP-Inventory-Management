package com.erp.inventory.service;

import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.response.OrderMaterialRequirementResponse;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.entity.Material;
import com.erp.inventory.entity.Order;
import com.erp.inventory.entity.OrderFulfillmentEvent;
import com.erp.inventory.entity.Party;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.ProductComponent;
import com.erp.inventory.entity.enums.FulfillmentStage;
import com.erp.inventory.repository.OrderRepository;
import com.erp.inventory.repository.PartyRepository;
import com.erp.inventory.repository.ProductComponentRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.ResourceRepository;
import com.erp.inventory.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderRepository orderRepository;
    @Mock PartyRepository partyRepository;
    @Mock ProductRepository productRepository;
    @Mock ProductComponentRepository productComponentRepository;
    @Mock ResourceRepository resourceRepository;
    @Mock MaterialStockMovementService materialStockMovementService;

    @InjectMocks OrderServiceImpl service;

    private Product product;
    private ProductComponent bomLine;

    @BeforeEach
    void setUp() {
        product = Product.builder().id(1L).name("YH134").build();
        Material yarn = Material.builder().id(10L).code("GOLD-150-POLY1").unit("kg").build();
        bomLine = ProductComponent.builder().id(100L).product(product).slotNo("f1")
                .material(yarn).quantityPerUnit(new BigDecimal("0.5")).build();
    }

    @Test
    void computeRequirement_flagsShortfall() {
        when(productRepository.findOneById(1L)).thenReturn(java.util.Optional.of(product));
        when(productComponentRepository.findByProductId(1L)).thenReturn(List.of(bomLine));
        when(materialStockMovementService.availableQuantities(anyCollection()))
                .thenReturn(Map.of(10L, new BigDecimal("30")));

        OrderMaterialRequirementResponse req = service.computeRequirement(1L, null, new BigDecimal("100"));

        assertThat(req.isAnyShortfall()).isTrue();
        OrderMaterialRequirementResponse.Line line = req.getLines().get(0);
        assertThat(line.getRequiredQty()).isEqualByComparingTo("50");
        assertThat(line.getShortfallQty()).isEqualByComparingTo("20");
        assertThat(line.isSufficient()).isFalse();
    }

    @Test
    void addFulfillmentEvent_rejectsOverCompletion() {
        Order order = order(new BigDecimal("50"));
        when(orderRepository.findOneById(5L)).thenReturn(java.util.Optional.of(order));

        OrderFulfillmentEventRequest req = new OrderFulfillmentEventRequest();
        req.setStage(FulfillmentStage.COMPLETED);
        req.setQuantity(new BigDecimal("60"));
        req.setDate(LocalDate.now());

        assertThatThrownBy(() -> service.addFulfillmentEvent(5L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds");
    }

    @Test
    void status_isCompletedWhenDispatchedAndSettled() {
        Order order = order(new BigDecimal("50"));
        order.addFulfillmentEvent(event(FulfillmentStage.COMPLETED, "50"));
        order.addFulfillmentEvent(event(FulfillmentStage.DISPATCHED, "50"));
        order.addFulfillmentEvent(event(FulfillmentStage.SETTLED, "50"));
        when(orderRepository.findOneById(5L)).thenReturn(java.util.Optional.of(order));
        when(productComponentRepository.findByProductId(1L)).thenReturn(List.of());
        when(materialStockMovementService.availableQuantities(anyCollection())).thenReturn(Map.of());

        OrderResponse res = service.getById(5L);
        assertThat(res.getStatus()).isEqualTo("COMPLETED");
        assertThat(res.getPendingQty()).isEqualByComparingTo("0");
    }

    private Order order(BigDecimal orderedQty) {
        return Order.builder()
                .id(5L).orderNumber("ORD-1").party(Party.builder().id(2L).name("Maulik").build())
                .product(product).date(LocalDate.now()).orderedQty(orderedQty)
                .build();
    }

    private OrderFulfillmentEvent event(FulfillmentStage stage, String qty) {
        return OrderFulfillmentEvent.builder().stage(stage).quantity(new BigDecimal(qty))
                .date(LocalDate.now()).build();
    }
}
