package com.erp.inventory.service.whatsapp;

import com.erp.inventory.dto.request.MaterialStockMovementRequest;
import com.erp.inventory.dto.request.OrderFulfillmentEventRequest;
import com.erp.inventory.dto.request.OrderRequest;
import com.erp.inventory.dto.request.PartyRequest;
import com.erp.inventory.dto.response.MaterialStockMovementResponse;
import com.erp.inventory.dto.response.OrderResponse;
import com.erp.inventory.dto.response.PartyResponse;
import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.WhatsappUserLink;
import com.erp.inventory.entity.enums.MovementDirection;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.exception.DuplicateResourceException;
import com.erp.inventory.exception.QuickEntryExecException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickEntryExecutorTest {

    @Mock OrderService orderService;
    @Mock MaterialStockMovementService materialStockMovementService;
    @Mock PartyService partyService;
    @Mock ProductService productService;
    @Mock BrokerService brokerService;
    @Mock AreaService areaService;
    @Mock ScheduleService scheduleService;
    @Mock JobService jobService;
    @Mock DesignService designService;
    @Mock TransporterService transporterService;
    @Mock VehicleService vehicleService;

    QuickEntryExecutor executor() {
        return new QuickEntryExecutor(orderService, materialStockMovementService, partyService, productService,
                brokerService, areaService, scheduleService, jobService, designService, transporterService,
                vehicleService);
    }

    private final WhatsappUserLink link = WhatsappUserLink.builder()
            .user(User.builder().id(3L).username("admin").build())
            .company(Company.builder().id(9L).name("Acme").build())
            .build();

    @Test
    void createOrder_mapsFieldsAndReturnsOrderNumber() {
        Map<String, Object> fields = new HashMap<>(Map.of(
                "partyId", 1, "productId", 5, "orderedQty", new BigDecimal("1250"), "date", "2026-08-30"));
        when(orderService.create(any(), any()))
                .thenReturn(OrderResponse.builder().orderNumber("ORD-123").build());

        String msg = executor().execute(QuickEntryAction.CREATE_ORDER, fields, link);

        ArgumentCaptor<OrderRequest> captor = ArgumentCaptor.forClass(OrderRequest.class);
        verify(orderService).create(captor.capture(), any());
        assertThat(captor.getValue().getPartyId()).isEqualTo(1L);
        assertThat(captor.getValue().getOrderedQty()).isEqualByComparingTo("1250");
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.of(2026, 8, 30));
        assertThat(msg).contains("ORD-123");
    }

    @Test
    void recordPurchase_forcesDirectionInAndDefaultsDate() {
        Map<String, Object> fields = new HashMap<>(Map.of(
                "materialId", 2, "partyId", 4, "quantity", new BigDecimal("200")));
        when(materialStockMovementService.create(any()))
                .thenReturn(MaterialStockMovementResponse.builder().id(55L).build());

        executor().execute(QuickEntryAction.RECORD_YARN_PURCHASE, fields, link);

        ArgumentCaptor<MaterialStockMovementRequest> captor =
                ArgumentCaptor.forClass(MaterialStockMovementRequest.class);
        verify(materialStockMovementService).create(captor.capture());
        assertThat(captor.getValue().getDirection()).isEqualTo(MovementDirection.IN);
        assertThat(captor.getValue().getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void addFulfillmentEvent_pullsOrderIdFromMapIntoPathArgument() {
        Map<String, Object> fields = new HashMap<>(Map.of(
                "orderId", 42, "stage", "DISPATCHED", "quantity", new BigDecimal("300"), "date", "2026-08-30"));
        when(orderService.addFulfillmentEvent(eq(42L), any()))
                .thenReturn(OrderResponse.builder().orderNumber("ORD-42").build());

        String msg = executor().execute(QuickEntryAction.ADD_FULFILLMENT_EVENT, fields, link);

        ArgumentCaptor<OrderFulfillmentEventRequest> captor =
                ArgumentCaptor.forClass(OrderFulfillmentEventRequest.class);
        verify(orderService).addFulfillmentEvent(eq(42L), captor.capture());
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("300");
        assertThat(msg).contains("ORD-42");
    }

    @Test
    void createParty_injectsCompanyFromLink() {
        Map<String, Object> fields = new HashMap<>(Map.of("name", "ABC Textiles", "roles", List.of("CUSTOMER")));
        when(partyService.create(any()))
                .thenReturn(PartyResponse.builder().id(8L).name("ABC Textiles").build());

        executor().execute(QuickEntryAction.CREATE_PARTY, fields, link);

        ArgumentCaptor<PartyRequest> captor = ArgumentCaptor.forClass(PartyRequest.class);
        verify(partyService).create(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo(9L);
    }

    @Test
    void serviceException_isWrappedAsQuickEntryExecException() {
        Map<String, Object> fields = new HashMap<>(Map.of(
                "sku", "SK12", "name", "Dupe", "unitPrice", new BigDecimal("120"), "costPrice", new BigDecimal("80")));
        when(productService.create(any())).thenThrow(new DuplicateResourceException("SKU SK12 already exists"));

        assertThatThrownBy(() -> executor().execute(QuickEntryAction.CREATE_PRODUCT, fields, link))
                .isInstanceOf(QuickEntryExecException.class)
                .hasMessageContaining("SK12 already exists");
    }
}
