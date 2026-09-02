package com.erp.inventory.service.quickentry;

import com.erp.inventory.dto.response.QuickEntryResult;
import com.erp.inventory.entity.enums.FulfillmentStage;
import com.erp.inventory.entity.enums.QuickEntryAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic first pass over a quick-note line: keyword -> action, regex -> numbers/dates,
 *  anchor words -> name fragments (resolved to ids via {@link EntityResolver}). */
@Component
@RequiredArgsConstructor
public class RuleBasedParser {

    private final EntityResolver resolver;

    private static final Pattern ORDER_NO = Pattern.compile("\\bord[-\\s]?\\d{2,}[-\\d]*\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern QTY = Pattern.compile("(?<![@₹\\d.])\\b(\\d+(?:\\.\\d+)?)\\s*(kg|kgs|pcs|pc|pieces|mtr|m)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY = Pattern.compile("(?:@|₹|rs\\.?\\s?|rate\\s+|price\\s+)(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PRICE_PAIR = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+\\.[\\w.-]+");
    private static final Pattern MOBILE = Pattern.compile("\\b(\\d{10})\\b");
    private static final Pattern ISO_DATE = Pattern.compile("\\b(\\d{4})-(\\d{2})-(\\d{2})\\b");
    private static final Pattern DAY_MONTH = Pattern.compile("\\b(\\d{1,2})\\s+([a-z]{3,})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern VARIANT = Pattern.compile("\\b(ground|pallu|border|body|skirt|blouse)\\s+([a-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern KV = Pattern.compile("\\b(pick|reed|hook|panna|gsm|pieces|weave)\\s+([a-z0-9-]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GSTIN = Pattern.compile("\\b(\\d{2}[A-Z]{5}\\d{4}[A-Z][A-Z0-9]Z[A-Z0-9])\\b", Pattern.CASE_INSENSITIVE);

    /** Trigger word -> "create simple master" action (name-only entities). */
    private static final Map<String, QuickEntryAction> MASTER_KEYWORDS = new LinkedHashMap<>() {{
        put("broker", QuickEntryAction.CREATE_BROKER);
        put("area", QuickEntryAction.CREATE_AREA);
        put("schedule", QuickEntryAction.CREATE_SCHEDULE);
        put("transporter", QuickEntryAction.CREATE_TRANSPORTER);
        put("vehicle", QuickEntryAction.CREATE_VEHICLE);
        put("pattern", QuickEntryAction.CREATE_DESIGN);
    }};

    public QuickEntryResult parse(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        QuickEntryAction action = detectAction(lower);
        QuickEntryResult r = QuickEntryResult.builder().action(action).originalText(text).build();
        switch (action) {
            case CREATE_ORDER -> parseOrder(text, lower, r);
            case RECORD_YARN_PURCHASE -> parsePurchase(text, lower, r);
            case ADD_FULFILLMENT_EVENT -> parseFulfillment(text, lower, r);
            case CREATE_PARTY -> parseParty(text, lower, r);
            case CREATE_PRODUCT -> parseProduct(text, lower, r);
            case CREATE_BROKER, CREATE_AREA, CREATE_SCHEDULE, CREATE_JOB,
                 CREATE_DESIGN, CREATE_TRANSPORTER, CREATE_VEHICLE -> parseSimpleMaster(text, lower, action, r);
            default -> r.setConfidence(0.1);
        }
        return r;
    }

    // --- intent -----------------------------------------------------------
    private QuickEntryAction detectAction(String s) {
        if (s.matches(".*\\bnew\\s+(customer|supplier|manufacturer|party)\\b.*")
                || s.matches(".*\\badd\\s+(customer|supplier|manufacturer|party)\\b.*")) {
            return QuickEntryAction.CREATE_PARTY;
        }
        // "new design master X" / "new pattern X" -> the Design master (before the product rule below)
        if (s.matches(".*\\b(new|add)\\s+design\\s+master\\b.*") || s.matches(".*\\b(new|add)\\s+pattern\\b.*")) {
            return QuickEntryAction.CREATE_DESIGN;
        }
        // "new job X" (but not "job worker" -> that's a party role, handled above)
        if (s.matches(".*\\b(new|add)\\s+job\\b.*")
                && !s.contains("job worker") && !s.contains("jobworker") && !s.contains("job work")) {
            return QuickEntryAction.CREATE_JOB;
        }
        for (Map.Entry<String, QuickEntryAction> e : MASTER_KEYWORDS.entrySet()) {
            if (s.matches(".*\\b(new|add)\\s+" + e.getKey() + "\\b.*")) {
                return e.getValue();
            }
        }
        if (s.startsWith("new design") || s.startsWith("new product") || s.startsWith("design ")
                || s.startsWith("add design")) {
            return QuickEntryAction.CREATE_PRODUCT;
        }
        if (s.matches(".*\\b(bought|buy|purchased?|received|recd|got|inward)\\b.*")) {
            return QuickEntryAction.RECORD_YARN_PURCHASE;
        }
        boolean stage = s.matches(".*\\b(completed|complete|dispatched|dispatch|settled|settle|returned|return)\\b.*");
        if ((stage || ORDER_NO.matcher(s).find()) && !s.startsWith("order ") && !s.contains("new order")) {
            return QuickEntryAction.ADD_FULFILLMENT_EVENT;
        }
        if (s.matches(".*\\border(ed|s)?\\b.*")) {
            return QuickEntryAction.CREATE_ORDER;
        }
        return QuickEntryAction.UNKNOWN;
    }

    // --- per-action ------------------------------------------------------
    private void parseOrder(String text, String lower, QuickEntryResult r) {
        Map<String, Object> f = r.getFields();
        firstQty(text).ifPresent(q -> f.put("orderedQty", q));
        money(lower).ifPresent(m -> f.put("rate", m));
        f.put("date", date(lower).toString());

        String partyPhrase = phraseAfter(lower, List.of("for", "to", "customer", "party", "buyer"));
        resolve(r, "PARTY", partyPhrase, resolver::party).ifPresent(id -> f.put("partyId", id));

        String prodPhrase = firstOr(phraseAfter(lower, List.of("design", "sku", "product", "item")),
                bestTokenMatch(text, this::resolveProductQuiet));
        resolve(r, "PRODUCT", prodPhrase, resolver::product).ifPresent(id -> f.put("productId", id));

        Map<String, Object> variant = new LinkedHashMap<>();
        Matcher vm = VARIANT.matcher(text);
        while (vm.find()) {
            variant.put(vm.group(1).toLowerCase() + "Color", vm.group(2));
        }
        if (!variant.isEmpty()) {
            f.put("variant", variant);
        }
        r.setConfidence(explicit(lower, "order") ? 0.9 : 0.6);
    }

    private void parsePurchase(String text, String lower, QuickEntryResult r) {
        Map<String, Object> f = r.getFields();
        f.put("direction", "IN");
        firstQty(text).ifPresent(q -> f.put("quantity", q));
        money(lower).ifPresent(m -> f.put("price", m));
        f.put("date", date(lower).toString());
        after(lower, List.of("inv", "invoice", "inv#", "bill", "bill#"))
                .ifPresent(v -> f.put("invoiceNo", v.toUpperCase(Locale.ROOT)));
        after(lower, List.of("lot", "lot#", "batch"))
                .ifPresent(v -> f.put("lotNo", v.toUpperCase(Locale.ROOT)));

        String partyPhrase = phraseAfter(lower, List.of("from", "supplier"));
        resolve(r, "PARTY", partyPhrase, resolver::party).ifPresent(id -> f.put("partyId", id));

        String matPhrase = firstOr(phraseAfter(lower, List.of("of", "yarn", "material")),
                bestTokenMatch(text, this::resolveMaterialQuiet));
        resolve(r, "MATERIAL", matPhrase, resolver::material).ifPresent(id -> f.put("materialId", id));
        r.setConfidence(0.85);
    }

    private void parseFulfillment(String text, String lower, QuickEntryResult r) {
        Map<String, Object> f = r.getFields();
        stage(lower).ifPresent(st -> f.put("stage", st.name()));
        // strip the order-number token first so its digits aren't read as a quantity
        firstQty(ORDER_NO.matcher(text).replaceAll(" ")).ifPresent(q -> f.put("quantity", q));
        f.put("date", date(lower).toString());

        Matcher on = ORDER_NO.matcher(text);
        String orderPhrase = on.find() ? on.group() : phraseAfter(lower, List.of("order"));
        resolve(r, "ORDER", orderPhrase, resolver::order).ifPresent(id -> f.put("orderId", id));

        String resPhrase = phraseAfter(lower, List.of("on", "loom", "machine", "resource"));
        resolve(r, "RESOURCE", resPhrase, resolver::resource).ifPresent(id -> f.put("resourceId", id));
        r.setConfidence(f.containsKey("stage") ? 0.85 : 0.55);
    }

    private void parseParty(String text, String lower, QuickEntryResult r) {
        Map<String, Object> f = r.getFields();
        String name = phraseAfter(text.toLowerCase(Locale.ROOT).replaceAll("\\b(new|add)\\b", ""),
                List.of("customer", "supplier", "manufacturer", "party", "buyer"));
        // keep original casing for the name
        if (!name.isBlank()) {
            f.put("name", titleFromOriginal(text, name));
        }
        List<String> roles = new ArrayList<>();
        if (lower.contains("customer") || lower.contains("buyer")) roles.add("CUSTOMER");
        if (lower.contains("supplier") || lower.contains("vendor")) roles.add("SUPPLIER");
        if (lower.contains("manufacturer")) roles.add("MANUFACTURER");
        if (roles.isEmpty()) roles.add("CUSTOMER");
        f.put("roles", roles);
        Matcher mob = MOBILE.matcher(text);
        if (mob.find()) f.put("mobile", mob.group(1));
        Matcher em = EMAIL.matcher(text);
        if (em.find()) f.put("email", em.group());
        r.setConfidence(0.8);
    }

    private void parseProduct(String text, String lower, QuickEntryResult r) {
        Map<String, Object> f = r.getFields();
        Matcher pair = PRICE_PAIR.matcher(text);
        if (pair.find()) {
            f.put("unitPrice", new BigDecimal(pair.group(1)));
            f.put("costPrice", new BigDecimal(pair.group(2)));
        } else {
            after(lower, List.of("price", "mrp")).map(BigDecimal::new).ifPresent(v -> f.put("unitPrice", v));
            after(lower, List.of("cost")).map(BigDecimal::new).ifPresent(v -> f.put("costPrice", v));
        }
        // sku = first token that looks like a code (letters+digits), name = rest
        String body = text.replaceAll("(?i)\\b(new\\s+)?(design|product)\\b", "").trim();
        String[] words = body.split("\\s+");
        for (String w : words) {
            if (w.matches("(?i)[a-z]{1,4}\\d{2,}[a-z0-9()-]*")) {
                f.put("sku", w.toUpperCase(Locale.ROOT));
                break;
            }
        }
        String name = body.replaceAll("\\d+\\s*/\\s*\\d+", "").replaceAll("(?i)\\b(pick|reed|hook|panna|gsm)\\s+\\w+", "").trim();
        if (!name.isBlank()) {
            f.put("name", name);
        }
        Map<String, Object> attrs = new LinkedHashMap<>();
        Matcher km = KV.matcher(text);
        while (km.find()) {
            attrs.put(km.group(1).toLowerCase(), coerce(km.group(2)));
        }
        if (!attrs.isEmpty()) f.put("attributes", attrs);
        r.setConfidence(0.7);
    }

    /** Name-only masters (Broker, Area, Schedule, Job, Design, Transporter, Vehicle) plus a few
     *  optional fields we can pull cheaply from the note. */
    private void parseSimpleMaster(String text, String lower, QuickEntryAction action, QuickEntryResult r) {
        Map<String, Object> f = r.getFields();

        String triggerWords = "new|add|broker|area|schedule|job|transporter|vehicle|pattern|design|master";
        String cleaned = lower
                .replaceAll(EMAIL.pattern(), " ")
                .replaceAll(MOBILE.pattern(), " ")
                .replaceAll("(?i)" + GSTIN.pattern(), " ")
                .replaceAll("(?i)\\b(rate|brokerage|gstin?|charge|tds|mobile|phone|ph|no|number)\\b.*", " ")
                .replaceAll("(?i)\\s(?:@|rs\\.?\\s?|₹|price\\s+)\\d[\\d.]*", " ")
                .replaceAll("(?i)\\b(" + triggerWords + ")\\b", " ")
                .replaceAll("[^a-z0-9 &.'-]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (!cleaned.isBlank()) {
            // vehicle "numbers" are always upper-case; other masters get title case
            f.put("name", action == QuickEntryAction.CREATE_VEHICLE
                    ? cleaned.toUpperCase(Locale.ROOT).replaceAll("\\s+", "")
                    : capitalizeWords(cleaned));
        }

        Matcher mob = MOBILE.matcher(text);
        if (mob.find()) {
            String phone = mob.group(1);
            switch (action) {
                case CREATE_BROKER -> f.put("phone1", phone);
                case CREATE_TRANSPORTER -> f.put("phone", phone);
                case CREATE_VEHICLE -> f.put("mobileNo", phone);
                case CREATE_AREA -> f.put("contactMobile", phone);
                default -> { /* no phone field */ }
            }
        }

        Matcher em = EMAIL.matcher(text);
        if (em.find() && action == QuickEntryAction.CREATE_BROKER) {
            f.put("email", em.group());
        }

        Matcher gm = GSTIN.matcher(text);
        if (gm.find() && action == QuickEntryAction.CREATE_TRANSPORTER) {
            f.put("gstin", gm.group(1).toUpperCase(Locale.ROOT));
        }

        after(lower, List.of("brokerage")).map(RuleBasedParser::num).filter(v -> v != null)
                .ifPresent(v -> { if (action == QuickEntryAction.CREATE_BROKER) f.put("brokeragePct", v); });

        money(lower).ifPresent(m -> {
            switch (action) {
                case CREATE_AREA, CREATE_JOB, CREATE_DESIGN -> f.put("rate", m);
                default -> { /* no rate field */ }
            }
        });

        r.setConfidence(f.containsKey("name") ? 0.82 : 0.35);
    }

    private static BigDecimal num(String s) {
        try {
            return new BigDecimal(s.replaceAll("[^0-9.]", ""));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String capitalizeWords(String s) {
        String[] w = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : w) {
            if (p.isBlank()) continue;
            sb.append(sb.length() == 0 ? "" : " ")
              .append(Character.toUpperCase(p.charAt(0)))
              .append(p.substring(1));
        }
        return sb.toString();
    }

    // --- helpers --------------------------------------------------------
    private Optional<Long> resolveProductQuiet(String s) {
        return resolver.product(s).map(EntityResolver.Match::id);
    }

    private Optional<Long> resolveMaterialQuiet(String s) {
        return resolver.material(s).map(EntityResolver.Match::id);
    }

    private Optional<Long> resolve(QuickEntryResult r, String kind, String phrase,
                                   java.util.function.Function<String, Optional<EntityResolver.Match>> fn) {
        if (phrase == null || phrase.isBlank()) {
            return Optional.empty();
        }
        Optional<EntityResolver.Match> m = fn.apply(phrase);
        m.ifPresent(match -> r.getResolved().add(QuickEntryResult.Resolved.builder()
                .token(phrase).kind(kind).id(match.id()).name(match.name()).score(match.score()).build()));
        if (m.isEmpty()) {
            r.getUnmatched().add(phrase);
        }
        return m.map(EntityResolver.Match::id);
    }

    /** Try each word and adjacent word-pair of the note; return the first that resolves. */
    private String bestTokenMatch(String text, java.util.function.Function<String, Optional<Long>> fn) {
        String[] w = text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 -]", " ").trim().split("\\s+");
        for (int n = 2; n >= 1; n--) {
            for (int i = 0; i + n <= w.length; i++) {
                String cand = String.join(" ", java.util.Arrays.copyOfRange(w, i, i + n));
                if (cand.length() < 3) continue;
                if (fn.apply(cand).isPresent()) {
                    return cand;
                }
            }
        }
        return "";
    }

    private static String firstOr(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static final List<String> STOPS = List.of("for", "from", "to", "at", "on", "rate", "price",
            "inv", "invoice", "lot", "@", "completed", "dispatched", "settled", "returned", "order",
            "design", "sku", "product", "customer", "supplier", "party", "of", "today", "yesterday", "tomorrow");

    private String phraseAfter(String lower, List<String> anchors) {
        String[] w = lower.split("\\s+");
        for (int i = 0; i < w.length; i++) {
            if (anchors.contains(w[i].replaceAll("[^a-z#]", ""))) {
                StringBuilder sb = new StringBuilder();
                for (int j = i + 1; j < w.length; j++) {
                    String t = w[j].replaceAll("[^a-z0-9@₹.-]", "");
                    if (t.isBlank() || STOPS.contains(t) || t.matches("[@₹]?\\d.*")) break;
                    sb.append(sb.length() == 0 ? "" : " ").append(t);
                }
                if (sb.length() > 0) return sb.toString().trim();
            }
        }
        return "";
    }

    private Optional<String> after(String lower, List<String> anchors) {
        String[] w = lower.split("\\s+");
        for (int i = 0; i < w.length - 1; i++) {
            String t = w[i].replaceAll("[^a-z#]", "");
            if (anchors.contains(t)) {
                String v = w[i + 1].replaceAll("[^A-Za-z0-9/-]", "");
                if (!v.isBlank()) return Optional.of(v);
            }
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> firstQty(String text) {
        Matcher m = QTY.matcher(text);
        if (m.find()) {
            return Optional.of(new BigDecimal(m.group(1)));
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> money(String lower) {
        Matcher m = MONEY.matcher(lower);
        return m.find() ? Optional.of(new BigDecimal(m.group(1))) : Optional.empty();
    }

    private Optional<FulfillmentStage> stage(String lower) {
        if (lower.matches(".*\\bcomplet.*")) return Optional.of(FulfillmentStage.COMPLETED);
        if (lower.matches(".*\\bdispatch.*")) return Optional.of(FulfillmentStage.DISPATCHED);
        if (lower.matches(".*\\bsettl.*")) return Optional.of(FulfillmentStage.SETTLED);
        if (lower.matches(".*\\breturn.*")) return Optional.of(FulfillmentStage.RETURNED);
        return Optional.empty();
    }

    private LocalDate date(String lower) {
        if (lower.contains("yesterday")) return LocalDate.now().minusDays(1);
        if (lower.contains("tomorrow")) return LocalDate.now().plusDays(1);
        Matcher iso = ISO_DATE.matcher(lower);
        if (iso.find()) {
            try {
                return LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)),
                        Integer.parseInt(iso.group(3)));
            } catch (RuntimeException ignored) {
                // fall through
            }
        }
        Matcher dm = DAY_MONTH.matcher(lower);
        if (dm.find()) {
            Integer month = monthOf(dm.group(2));
            if (month != null) {
                try {
                    return LocalDate.of(LocalDate.now().getYear(), month, Integer.parseInt(dm.group(1)));
                } catch (RuntimeException ignored) {
                    // fall through
                }
            }
        }
        return LocalDate.now();
    }

    private Integer monthOf(String s) {
        String p = s.toLowerCase(Locale.ROOT);
        for (java.time.Month m : java.time.Month.values()) {
            String full = m.getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();
            if (full.startsWith(p) || p.startsWith(full.substring(0, 3))) {
                return m.getValue();
            }
        }
        return null;
    }

    private static boolean explicit(String lower, String kw) {
        return lower.matches(".*\\b" + kw + "\\b.*");
    }

    private static Object coerce(String raw) {
        if (raw.matches("-?\\d+")) return Integer.parseInt(raw);
        if (raw.matches("-?\\d*\\.\\d+")) return new BigDecimal(raw);
        return raw;
    }

    /** Recover the original-cased span for a lowercased fragment. */
    private static String titleFromOriginal(String original, String lowerFragment) {
        int idx = original.toLowerCase(Locale.ROOT).indexOf(lowerFragment);
        return idx >= 0 ? original.substring(idx, idx + lowerFragment.length()).trim() : lowerFragment;
    }
}
