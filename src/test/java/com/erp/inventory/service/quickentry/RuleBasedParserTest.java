package com.erp.inventory.service.quickentry;

import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.entity.enums.QuickEntryAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RuleBasedParserTest {

    @Mock EntityResolver resolver;
    RuleBasedParser parser;

    @BeforeEach
    void setUp() {
        parser = new RuleBasedParser(resolver);
        when(resolver.party(anyString())).thenReturn(Optional.empty());
        when(resolver.product(anyString())).thenReturn(Optional.empty());
        when(resolver.material(anyString())).thenReturn(Optional.empty());
        when(resolver.order(anyString())).thenReturn(Optional.empty());
        when(resolver.resource(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void order_intentAndFields() {
        lenient().when(resolver.party(contains("maulik")))
                .thenReturn(Optional.of(new EntityResolver.Match(1L, "Maulik Textiles", 0.9)));
        lenient().when(resolver.product(contains("yh134")))
                .thenReturn(Optional.of(new EntityResolver.Match(6L, "YH134", 1.0)));

        QuickEntryResult r = parser.parse("order 300 YH134 for maulik @850 ground red pallu gold");

        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_ORDER);
        assertThat(r.getFields()).containsEntry("orderedQty", new BigDecimal("300"));
        assertThat(r.getFields()).containsEntry("rate", new BigDecimal("850"));
        assertThat(r.getFields()).containsEntry("partyId", 1L);
        assertThat(r.getFields()).containsEntry("productId", 6L);
        assertThat(r.getFields()).containsEntry("date", LocalDate.now().toString());
        @SuppressWarnings("unchecked")
        var variant = (java.util.Map<String, Object>) r.getFields().get("variant");
        assertThat(variant).containsEntry("groundColor", "red").containsEntry("palluColor", "gold");
    }

    @Test
    void yarnPurchase_intentAndFields() {
        QuickEntryResult r = parser.parse("bought 500kg RED-150-POLY1 from ambica inv AY-2210 @172");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.RECORD_YARN_PURCHASE);
        assertThat(r.getFields()).containsEntry("direction", "IN");
        assertThat(r.getFields()).containsEntry("quantity", new BigDecimal("500"));
        assertThat(r.getFields()).containsEntry("price", new BigDecimal("172"));
        assertThat(r.getFields()).containsEntry("invoiceNo", "AY-2210");
    }

    @Test
    void fulfillment_intentAndStage() {
        QuickEntryResult r = parser.parse("ORD-2026-004 completed 200 on loom 3");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.ADD_FULFILLMENT_EVENT);
        assertThat(r.getFields()).containsEntry("stage", "COMPLETED");
        assertThat(r.getFields()).containsEntry("quantity", new BigDecimal("200"));
    }

    @Test
    void party_intentAndRoles() {
        QuickEntryResult r = parser.parse("new customer Shreeji Sarees 9825011122");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_PARTY);
        assertThat(r.getFields()).containsEntry("name", "Shreeji Sarees");
        assertThat(r.getFields()).containsEntry("mobile", "9825011122");
        assertThat(r.getFields().get("roles").toString()).isEqualTo("[CUSTOMER]");
    }

    @Test
    void product_intentAndPricePair() {
        QuickEntryResult r = parser.parse("new design YH500 saree 900/550 pick 44 reed 120");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_PRODUCT);
        assertThat(r.getFields()).containsEntry("sku", "YH500");
        assertThat(r.getFields()).containsEntry("unitPrice", new BigDecimal("900"));
        assertThat(r.getFields()).containsEntry("costPrice", new BigDecimal("550"));
        @SuppressWarnings("unchecked")
        var attrs = (java.util.Map<String, Object>) r.getFields().get("attributes");
        assertThat(attrs).containsEntry("pick", 44).containsEntry("reed", 120);
    }

    @Test
    void broker_intentNameAndPhone() {
        QuickEntryResult r = parser.parse("new broker Maulik Shah 9825011122 brokerage 2");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_BROKER);
        assertThat(r.getFields()).containsEntry("name", "Maulik Shah");
        assertThat(r.getFields()).containsEntry("phone1", "9825011122");
        assertThat(r.getFields()).containsEntry("brokeragePct", new BigDecimal("2"));
        assertThat(r.getConfidence()).isGreaterThan(0.5);
    }

    @Test
    void transporter_intentNameAndGstin() {
        QuickEntryResult r = parser.parse("add transporter VRL Logistics 24AAZPJ7525L1Z0");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_TRANSPORTER);
        assertThat(r.getFields()).containsEntry("name", "Vrl Logistics");
        assertThat(r.getFields()).containsEntry("gstin", "24AAZPJ7525L1Z0");
    }

    @Test
    void area_intentAndRate() {
        QuickEntryResult r = parser.parse("new area Ring Road rate 50");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_AREA);
        assertThat(r.getFields()).containsEntry("name", "Ring Road");
        assertThat(r.getFields()).containsEntry("rate", new BigDecimal("50"));
    }

    @Test
    void newDesign_withoutMasterKeyword_isStillProduct() {
        QuickEntryResult r = parser.parse("new design YH500 saree 900/550");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_PRODUCT);
    }

    @Test
    void designMaster_intent() {
        QuickEntryResult r = parser.parse("new pattern Peacock Butta");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.CREATE_DESIGN);
        assertThat(r.getFields()).containsEntry("name", "Peacock Butta");
    }

    @Test
    void gibberish_isUnknown() {
        QuickEntryResult r = parser.parse("asdf qwer zxcv");
        assertThat(r.getAction()).isEqualTo(QuickEntryAction.UNKNOWN);
        assertThat(r.getConfidence()).isLessThan(0.3);
    }
}
