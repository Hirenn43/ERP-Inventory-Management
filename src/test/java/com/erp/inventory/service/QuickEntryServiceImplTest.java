package com.erp.inventory.service;

import com.erp.inventory.dto.request.QuickEntryParseRequest;
import com.erp.inventory.dto.response.OrderMaterialRequirementResponse;
import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.service.impl.QuickEntryServiceImpl;
import com.erp.inventory.service.quickentry.RequiredFields;
import com.erp.inventory.service.quickentry.RuleBasedParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuickEntryServiceImplTest {

    @Mock RuleBasedParser ruleBasedParser;
    @Mock OrderService orderService;
    @Spy RequiredFields requiredFields = new RequiredFields();
    @InjectMocks QuickEntryServiceImpl service;

    private QuickEntryParseRequest req(String text) {
        QuickEntryParseRequest r = new QuickEntryParseRequest();
        r.setText(text);
        return r;
    }

    @Test
    void missing_fieldsListedAndConfidenceDropped() {
        ReflectionTestUtils.setField(service, "autoSubmitThreshold", 0.85);
        QuickEntryResult raw = QuickEntryResult.builder()
                .action(QuickEntryAction.CREATE_ORDER)
                .confidence(0.9)
                .build();
        raw.getFields().put("orderedQty", new BigDecimal("300")); // partyId + productId missing
        when(ruleBasedParser.parse(any())).thenReturn(raw);

        QuickEntryResult out = service.parse(req("order 300"));

        assertThat(out.getMissing()).containsExactlyInAnyOrder("partyId", "productId");
        assertThat(out.getConfidence()).isLessThan(0.5);
        assertThat(out.getAutoSubmitThreshold()).isEqualTo(0.85);
    }

    @Test
    void order_shortfallWarningFoldedIn() {
        QuickEntryResult raw = QuickEntryResult.builder().action(QuickEntryAction.CREATE_ORDER).confidence(0.9).build();
        raw.getFields().putAll(Map.of("partyId", 1L, "productId", 5L, "orderedQty", new BigDecimal("900")));
        when(ruleBasedParser.parse(any())).thenReturn(raw);
        when(orderService.computeRequirement(anyLong(), any(), any())).thenReturn(
                OrderMaterialRequirementResponse.builder()
                        .anyShortfall(true)
                        .lines(List.of(OrderMaterialRequirementResponse.Line.builder()
                                .materialCode("SILV-150-ZARI")
                                .shortfallQty(new BigDecimal("15.0000"))
                                .sufficient(false)
                                .quantityPerUnitMissing(false)
                                .build()))
                        .build());

        QuickEntryResult out = service.parse(req("order 900 YH301 for maulik"));

        assertThat(out.getMissing()).isEmpty();
        assertThat(out.getWarnings()).anySatisfy(w -> assertThat(w).contains("SILV-150-ZARI").contains("15"));
    }
}
