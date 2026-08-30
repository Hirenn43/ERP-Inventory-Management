package com.erp.inventory.service.whatsapp;

import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.WhatsappUserLink;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.repository.WhatsappUserLinkRepository;
import com.erp.inventory.service.QuickEntryService;
import com.erp.inventory.service.quickentry.EntityResolver;
import com.erp.inventory.service.quickentry.RequiredFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WhatsAppQuickEntryServiceTest {

    private static final String PHONE = "919876543210";

    @Mock WhatsappUserLinkRepository linkRepository;
    @Mock QuickEntryService quickEntryService;
    @Mock FieldInterpreter interpreter;
    @Mock QuickEntryExecutor executor;

    InMemoryWaConversationStore store;
    WhatsAppQuickEntryService service;
    WhatsappUserLink link;

    @BeforeEach
    void setUp() {
        store = new InMemoryWaConversationStore();
        service = new WhatsAppQuickEntryService(
                linkRepository, store, quickEntryService,
                new RequiredFields(), new FieldPrompts(), interpreter, executor);
        link = WhatsappUserLink.builder()
                .id(1L).phone(PHONE).enabled(true)
                .user(User.builder().id(7L).username("admin").build())
                .build();
    }

    private void registered() {
        when(linkRepository.findByPhoneAndEnabledTrue(PHONE)).thenReturn(Optional.of(link));
    }

    private QuickEntryResult parsed(QuickEntryAction action, Map<String, Object> fields) {
        QuickEntryResult r = QuickEntryResult.builder().action(action).build();
        r.getFields().putAll(fields);
        return r;
    }

    @Test
    void unregisteredNumber_isRejected() {
        when(linkRepository.findByPhoneAndEnabledTrue(PHONE)).thenReturn(Optional.empty());

        String reply = service.handleInbound("+91 98765 43210", "new order");

        assertThat(reply).contains("isn't registered");
        verify(quickEntryService, never()).parse(any());
    }

    @Test
    void unknownIntent_returnsHelp() {
        registered();
        when(quickEntryService.parse(any())).thenReturn(parsed(QuickEntryAction.UNKNOWN, Map.of()));

        String reply = service.handleInbound(PHONE, "hello there");

        assertThat(reply).contains("couldn't tell what to record");
        assertThat(store.has(PHONE)).isFalse();
    }

    @Test
    void missingFields_askedOneAtATime_thenExecuted() {
        registered();
        when(quickEntryService.parse(any())).thenReturn(parsed(QuickEntryAction.CREATE_ORDER, Map.of()));

        // 1. first message -> ask for party
        String q1 = service.handleInbound(PHONE, "new order");
        assertThat(q1).isEqualTo("Customer name?");
        assertThat(store.has(PHONE)).isTrue();

        // 2. party answer resolves to id 1 -> ask for product
        when(interpreter.interpret(eq("partyId"), any()))
                .thenReturn(FieldInterpreter.Outcome.filled(1L));
        String q2 = service.handleInbound(PHONE, "ABC Textiles");
        assertThat(q2).isEqualTo("Design / SKU?");

        // 3. product answer -> ask for quantity
        when(interpreter.interpret(eq("productId"), any()))
                .thenReturn(FieldInterpreter.Outcome.filled(5L));
        String q3 = service.handleInbound(PHONE, "SK12");
        assertThat(q3).isEqualTo("Quantity (meters)?");

        // 4. quantity -> all present -> execute
        when(interpreter.interpret(eq("orderedQty"), any()))
                .thenReturn(FieldInterpreter.Outcome.filled(new BigDecimal("1250")));
        when(executor.execute(eq(QuickEntryAction.CREATE_ORDER), any(), eq(link)))
                .thenReturn("Entry completed ✅ ORD-17");
        String done = service.handleInbound(PHONE, "1250");

        assertThat(done).isEqualTo("Entry completed ✅ ORD-17");
        assertThat(store.has(PHONE)).isFalse();
    }

    @Test
    void ambiguousParty_showsMenu_thenPickResolves() {
        registered();
        when(quickEntryService.parse(any())).thenReturn(parsed(QuickEntryAction.CREATE_ORDER, Map.of()));
        service.handleInbound(PHONE, "new order"); // -> asks partyId

        when(interpreter.interpret(eq("partyId"), any())).thenReturn(FieldInterpreter.Outcome.menu(
                "partyId", List.of(
                        new EntityResolver.Match(11L, "ABC Textiles", 0.7),
                        new EntityResolver.Match(12L, "ABC Traders", 0.68))));
        String menu = service.handleInbound(PHONE, "ABC");
        assertThat(menu).contains("1. ABC Textiles").contains("2. ABC Traders").contains("reply a number");

        // picking "2" fills partyId=12 and advances to the next question
        String next = service.handleInbound(PHONE, "2");
        assertThat(next).isEqualTo("Design / SKU?");
        assertThat(store.find(PHONE).orElseThrow().getFields()).containsEntry("partyId", 12L);
    }

    @Test
    void badFieldReply_retriesThenAbortsAfterThree() {
        registered();
        when(quickEntryService.parse(any())).thenReturn(parsed(QuickEntryAction.CREATE_ORDER, Map.of()));
        service.handleInbound(PHONE, "new order");

        when(interpreter.interpret(eq("partyId"), any()))
                .thenReturn(FieldInterpreter.Outcome.failed("No party matches that."));

        assertThat(service.handleInbound(PHONE, "???")).isEqualTo("No party matches that.");
        assertThat(service.handleInbound(PHONE, "???")).isEqualTo("No party matches that.");
        String third = service.handleInbound(PHONE, "???");
        assertThat(third).contains("cancelled");
        assertThat(store.has(PHONE)).isFalse();
    }

    @Test
    void cancel_dropsConversation() {
        registered();
        when(quickEntryService.parse(any())).thenReturn(parsed(QuickEntryAction.CREATE_ORDER, Map.of()));
        service.handleInbound(PHONE, "new order");

        String reply = service.handleInbound(PHONE, "cancel");

        assertThat(reply).contains("Cancelled");
        assertThat(store.has(PHONE)).isFalse();
    }

    @Test
    void fullySpecifiedNote_executesImmediately() {
        registered();
        when(quickEntryService.parse(any())).thenReturn(parsed(QuickEntryAction.CREATE_ORDER,
                Map.of("partyId", 1L, "productId", 5L, "orderedQty", new BigDecimal("500"))));
        when(executor.execute(eq(QuickEntryAction.CREATE_ORDER), any(), eq(link)))
                .thenReturn("Entry completed ✅ ORD-99");

        String reply = service.handleInbound(PHONE, "ordered 500 mtr for ABC design SK12");

        assertThat(reply).isEqualTo("Entry completed ✅ ORD-99");
        assertThat(store.has(PHONE)).isFalse();
    }
}
