package com.erp.inventory.service.quickentry;

import com.erp.inventory.entity.Material;
import com.erp.inventory.entity.Order;
import com.erp.inventory.entity.Party;
import com.erp.inventory.entity.Product;
import com.erp.inventory.entity.Resource;
import com.erp.inventory.repository.MaterialRepository;
import com.erp.inventory.repository.OrderRepository;
import com.erp.inventory.repository.PartyRepository;
import com.erp.inventory.repository.ProductRepository;
import com.erp.inventory.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Fuzzy-matches a free-text fragment from a quick note against master data. Datasets are small
 *  (single tenant) so it just loads all rows and scores them in memory. */
@Component
@RequiredArgsConstructor
public class EntityResolver {

    private final PartyRepository partyRepository;
    private final ProductRepository productRepository;
    private final MaterialRepository materialRepository;
    private final OrderRepository orderRepository;
    private final ResourceRepository resourceRepository;

    public record Match(Long id, String name, double score) {}

    /** Master-data type, for the generic {@link #candidates(String, Kind, int)} entry point. */
    public enum Kind { PARTY, PRODUCT, MATERIAL, ORDER, RESOURCE }

    private static final double ACCEPT = 0.55;
    /** best must beat the runner-up by this much, else it's ambiguous. */
    private static final double MARGIN = 0.05;
    /** Lowest score a row may have and still be offered as a pick-list candidate. */
    private static final double CANDIDATE_FLOOR = 0.4;

    public Optional<Match> party(String text) {
        return best(text, partyRepository.findAll(), Party::getId, p -> List.of(p.getName()));
    }

    public Optional<Match> product(String text) {
        return best(text, productRepository.findAll(), Product::getId,
                p -> List.of(p.getSku(), p.getName()));
    }

    public Optional<Match> material(String text) {
        return best(text, materialRepository.findAll(), Material::getId,
                m -> List.of(m.getCode(), m.getQuality() == null ? "" : m.getQuality()));
    }

    public Optional<Match> order(String text) {
        return best(text, orderRepository.findAll(), Order::getId, o -> List.of(o.getOrderNumber()));
    }

    public Optional<Match> resource(String text) {
        return best(text, resourceRepository.findAll(), Resource::getId, r -> List.of(r.getName()));
    }

    /** Top {@code limit} matches for {@code text} scoring at least {@link #CANDIDATE_FLOOR}, best first,
     *  WITHOUT the single-winner ambiguity gate - so a caller can present a numbered pick list. */
    public List<Match> candidates(String text, Kind kind, int limit) {
        return switch (kind) {
            case PARTY -> scored(text, partyRepository.findAll(), Party::getId,
                    p -> List.of(p.getName()), limit);
            case PRODUCT -> scored(text, productRepository.findAll(), Product::getId,
                    p -> List.of(p.getSku(), p.getName()), limit);
            case MATERIAL -> scored(text, materialRepository.findAll(), Material::getId,
                    m -> List.of(m.getCode(), m.getQuality() == null ? "" : m.getQuality()), limit);
            case ORDER -> scored(text, orderRepository.findAll(), Order::getId,
                    o -> List.of(o.getOrderNumber()), limit);
            case RESOURCE -> scored(text, resourceRepository.findAll(), Resource::getId,
                    r -> List.of(r.getName()), limit);
        };
    }

    private <T> List<Match> scored(String text, List<T> items, Function<T, Long> id,
                                   Function<T, List<String>> keys, int limit) {
        String q = normalize(text);
        if (q.isBlank() || items.isEmpty()) {
            return List.of();
        }
        List<Match> all = new ArrayList<>();
        for (T item : items) {
            double s = 0.0;
            for (String key : keys.apply(item)) {
                s = Math.max(s, score(q, normalize(key)));
            }
            all.add(new Match(id.apply(item), keys.apply(item).get(0), s));
        }
        all.sort((a, b) -> Double.compare(b.score(), a.score()));
        List<Match> above = all.stream().filter(m -> m.score() >= CANDIDATE_FLOOR).limit(limit).toList();
        if (above.size() <= 1) {
            return above;
        }
        Match top = above.get(0);
        // an exact hit, or one that clearly dominates the runner-up, is not really a choice
        if (Double.compare(top.score(), 1.0) == 0 || top.score() - above.get(1).score() >= 0.15) {
            return List.of(top);
        }
        return above;
    }

    private <T> Optional<Match> best(String text, List<T> items, Function<T, Long> id,
                                     Function<T, List<String>> keys) {
        String q = normalize(text);
        if (q.isBlank() || items.isEmpty()) {
            return Optional.empty();
        }
        List<Match> scored = new ArrayList<>();
        for (T item : items) {
            double s = 0.0;
            for (String key : keys.apply(item)) {
                s = Math.max(s, score(q, normalize(key)));
            }
            String label = keys.apply(item).get(0);
            scored.add(new Match(id.apply(item), label, s));
        }
        scored.sort((a, b) -> Double.compare(b.score(), a.score()));
        Match top = scored.get(0);
        if (top.score() < ACCEPT) {
            return Optional.empty();
        }
        if (scored.size() > 1 && top.score() - scored.get(1).score() < MARGIN
                && Double.compare(top.score(), 1.0) != 0) {
            return Optional.empty(); // ambiguous
        }
        return Optional.of(top);
    }

    static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
    }

    /** 1.0 exact; 0.9 one contains the other on a word boundary; else blended token-overlap + edit distance. */
    static double score(String q, String key) {
        if (key.isBlank()) {
            return 0.0;
        }
        if (q.equals(key)) {
            return 1.0;
        }
        if (containsWord(key, q) || containsWord(q, key)) {
            return 0.9;
        }
        double overlap = tokenOverlap(q, key);
        double edit = 1.0 - (double) levenshtein(q, key) / Math.max(q.length(), key.length());
        return Math.max(overlap * 0.85, edit * 0.75);
    }

    private static boolean containsWord(String haystack, String needle) {
        if (needle.isBlank()) {
            return false;
        }
        return (" " + haystack + " ").contains(" " + needle + " ");
    }

    private static double tokenOverlap(String a, String b) {
        List<String> ta = Arrays.asList(a.split(" "));
        List<String> tb = Arrays.asList(b.split(" "));
        if (ta.isEmpty() || tb.isEmpty()) {
            return 0.0;
        }
        long hit = ta.stream().filter(tb::contains).count();
        return (double) hit / Math.max(ta.size(), tb.size());
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }
}
