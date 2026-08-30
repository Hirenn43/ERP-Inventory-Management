package com.erp.inventory.service.whatsapp;

import com.erp.inventory.dto.request.QuickEntryParseRequest;
import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.entity.WhatsappUserLink;
import com.erp.inventory.entity.enums.QuickEntryAction;
import com.erp.inventory.exception.QuickEntryExecException;
import com.erp.inventory.repository.WhatsappUserLinkRepository;
import com.erp.inventory.service.QuickEntryService;
import com.erp.inventory.service.quickentry.EntityResolver;
import com.erp.inventory.service.quickentry.RequiredFields;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

/** Drives the WhatsApp question/answer loop: parse the first note, then ask for each missing
 *  required field one at a time, then create the record. Conversation state lives in Redis. */
@Service
@RequiredArgsConstructor
public class WhatsAppQuickEntryService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppQuickEntryService.class);
    private static final int MAX_FIELD_ATTEMPTS = 3;

    private final WhatsappUserLinkRepository linkRepository;
    private final WaConversationStore store;
    private final QuickEntryService quickEntryService;
    private final RequiredFields requiredFields;
    private final FieldPrompts prompts;
    private final FieldInterpreter interpreter;
    private final QuickEntryExecutor executor;

    /** Handle one inbound text message and return the reply to send back. Never throws. */
    public String handleInbound(String fromPhone, String text) {
        try {
            return dispatch(fromPhone, text);
        } catch (RuntimeException e) {
            log.error("WhatsApp quick-entry failed for {}", fromPhone, e);
            return "Something went wrong on our side. Please try again.";
        }
    }

    private String dispatch(String fromPhone, String text) {
        String phone = PhoneNumbers.digits(fromPhone);
        Optional<WhatsappUserLink> linkOpt = linkRepository.findByPhoneAndEnabledTrue(phone);
        if (linkOpt.isEmpty()) {
            return "This number isn't registered for quick entry. Ask your admin to add it.";
        }
        WhatsappUserLink link = linkOpt.get();

        String body = text == null ? "" : text.trim();
        if (body.length() > 1000) {
            body = body.substring(0, 1000);
        }
        String lower = body.toLowerCase();

        if (lower.equals("cancel") || lower.equals("stop") || lower.equals("quit")) {
            store.delete(phone);
            return "Cancelled ❌";
        }
        if (lower.equals("restart") || lower.equals("reset")) {
            store.delete(phone);
            return "Okay, starting over. What would you like to record?";
        }

        Optional<WaConversation> convOpt = store.find(phone);

        WaConversation conv;
        QuickEntryResult seed = null;
        if (convOpt.isEmpty()) {
            seed = quickEntryService.parse(parseRequest(body));
            if (seed.getAction() == QuickEntryAction.UNKNOWN) {
                return helpText();
            }
            conv = new WaConversation();
            conv.setPhone(phone);
            conv.setAction(seed.getAction());
            conv.setFields(new LinkedHashMap<>(seed.getFields()));
        } else {
            conv = convOpt.get();
            String interim = applyReply(conv, body);
            if (interim != null) {
                return interim;
            }
        }
        return advance(conv, link, seed);
    }

    /** Applies {@code body} to the field the bot is currently waiting on. Returns a reply string
     *  when another answer is still needed (menu shown, retry, or aborted); {@code null} once a
     *  field was accepted and the flow can advance. */
    private String applyReply(WaConversation conv, String body) {
        if (conv.getMenuField() != null) {
            Integer pick = parsePositiveInt(body);
            int size = conv.getMenuIds().size();
            if (pick == null || pick < 1 || pick > size) {
                return "Reply a number between 1 and " + size + ".";
            }
            conv.getFields().put(conv.getMenuField(), conv.getMenuIds().get(pick - 1));
            conv.clearMenu();
            conv.setCurrentField(null);
            conv.setFieldAttempts(0);
            return null;
        }

        String field = conv.getCurrentField();
        if (field == null) {
            return null;
        }

        FieldInterpreter.Outcome outcome = interpreter.interpret(field, body);
        switch (outcome.kind()) {
            case FILLED -> {
                conv.getFields().put(field, outcome.value());
                conv.setCurrentField(null);
                conv.setFieldAttempts(0);
                return null;
            }
            case MENU -> {
                conv.setMenuField(field);
                conv.setMenuIds(outcome.options().stream().map(EntityResolver.Match::id).toList());
                touch(conv);
                store.save(conv);
                return menuText(outcome.options());
            }
            case FAILED -> {
                conv.setFieldAttempts(conv.getFieldAttempts() + 1);
                if (conv.getFieldAttempts() >= MAX_FIELD_ATTEMPTS) {
                    store.delete(conv.getPhone());
                    return "Couldn't understand that. Entry cancelled — please start again.";
                }
                touch(conv);
                store.save(conv);
                return outcome.message();
            }
            default -> {
                return null;
            }
        }
    }

    private String advance(WaConversation conv, WhatsappUserLink link, QuickEntryResult seed) {
        List<String> missing = requiredFields.missing(conv.getAction(), conv.getFields());
        if (!missing.isEmpty()) {
            conv.setCurrentField(missing.get(0));
            touch(conv);
            store.save(conv);
            return prompts.prompt(conv.getAction(), missing.get(0));
        }

        try {
            String confirmation = executor.execute(conv.getAction(), conv.getFields(), link);
            store.delete(conv.getPhone());
            if (seed != null && seed.getWarnings() != null && !seed.getWarnings().isEmpty()) {
                confirmation += "\n" + String.join("\n", seed.getWarnings());
            }
            return confirmation;
        } catch (QuickEntryExecException e) {
            store.delete(conv.getPhone());
            return e.getMessage();
        }
    }

    private static void touch(WaConversation conv) {
        conv.setLastActivityAt(Instant.now());
    }

    private static QuickEntryParseRequest parseRequest(String text) {
        QuickEntryParseRequest request = new QuickEntryParseRequest();
        request.setText(text);
        return request;
    }

    private static Integer parsePositiveInt(String body) {
        String digits = body == null ? "" : body.trim().replaceAll("[^0-9]", "");
        if (digits.isEmpty() || digits.length() > 3) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private static String menuText(List<EntityResolver.Match> options) {
        StringBuilder sb = new StringBuilder("Which one?\n");
        for (int i = 0; i < options.size(); i++) {
            sb.append(i + 1).append(". ").append(options.get(i).name()).append('\n');
        }
        return sb.append("(reply a number)").toString();
    }

    private static String helpText() {
        return """
                I couldn't tell what to record. Try one of:
                • order — "ordered 500 mtr for ABC Textiles design SK12"
                • purchase — "bought 200 kg yarn from XYZ Traders"
                • dispatch — "dispatched 300 mtr ORD-123"
                • new party — "new customer ABC Textiles"
                • new design — "new design SK12 rate 120/80\"""";
    }
}
