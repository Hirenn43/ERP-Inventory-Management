package com.erp.inventory.service.impl;

import com.erp.inventory.dto.request.QuickEntryParseRequest;
import com.erp.inventory.dto.response.OrderMaterialRequirementResponse;
import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.service.OrderService;
import com.erp.inventory.service.QuickEntryService;
import com.erp.inventory.service.quickentry.RequiredFields;
import com.erp.inventory.service.quickentry.RuleBasedParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class QuickEntryServiceImpl implements QuickEntryService {

    private final RuleBasedParser ruleBasedParser;
    private final OrderService orderService;
    private final RequiredFields requiredFields;

    @Value("${app.quick-entry.auto-submit-threshold:0.85}")
    private double autoSubmitThreshold;

    @Override
    public QuickEntryResult parse(QuickEntryParseRequest request) {
        QuickEntryResult r = ruleBasedParser.parse(request.getText().trim());

        // TODO (step 6): if r.getConfidence() < llmThreshold and a key is configured, run LlmParser and merge.

        recomputeMissing(r);
        r.setConfidence(adjust(r.getConfidence(), r.getMissing().size()));
        if (r.getAction() == QuickEntryAction.CREATE_ORDER) {
            addOrderWarnings(r);
        }
        r.setAutoSubmitThreshold(autoSubmitThreshold);
        return r;
    }

    private void recomputeMissing(QuickEntryResult r) {
        r.getMissing().clear();
        r.getMissing().addAll(requiredFields.missing(r.getAction(), r.getFields()));
    }

    private double adjust(double base, int missingCount) {
        double v = base - 0.28 * missingCount;
        return Math.max(0.0, Math.min(1.0, v));
    }

    private void addOrderWarnings(QuickEntryResult r) {
        Object productId = r.getFields().get("productId");
        Object qty = r.getFields().get("orderedQty");
        if (!(productId instanceof Number pid) || !(qty instanceof BigDecimal q)) {
            return;
        }
        Object componentId = r.getFields().get("componentId");
        Long cid = componentId instanceof Number n ? n.longValue() : null;
        try {
            OrderMaterialRequirementResponse req = orderService.computeRequirement(pid.longValue(), cid, q);
            for (OrderMaterialRequirementResponse.Line line : req.getLines()) {
                if (!line.isSufficient() && !line.isQuantityPerUnitMissing()) {
                    r.getWarnings().add("Yarn " + line.getMaterialCode() + " short by "
                            + line.getShortfallQty().stripTrailingZeros().toPlainString());
                }
            }
        } catch (RuntimeException ignored) {
            // requirement is advisory only - never block the parse
        }
    }
}
