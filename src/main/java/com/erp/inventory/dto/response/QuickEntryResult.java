package com.erp.inventory.dto.response;

import com.erp.inventory.entity.enums.QuickEntryAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Interpretation of one quick-note line. Never writes anything - the frontend takes {@code fields}
 *  (already shaped like the matching *Request DTO, ids resolved) and calls the normal create endpoint,
 *  automatically when {@code confidence >= autoSubmitThreshold} and {@code missing} is empty. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuickEntryResult {

    @Builder.Default
    private QuickEntryAction action = QuickEntryAction.UNKNOWN;

    /** "RULES" or "LLM" - which parser produced this. */
    @Builder.Default
    private String engine = "RULES";

    @Builder.Default
    private double confidence = 0.0;

    /** Payload for the create call, keys matching the target *Request DTO. */
    @Builder.Default
    private Map<String, Object> fields = new LinkedHashMap<>();

    @Builder.Default
    private List<Resolved> resolved = new ArrayList<>();

    /** Required fields that could not be filled - the frontend must prompt for these. */
    @Builder.Default
    private List<String> missing = new ArrayList<>();

    /** Non-blocking advisories (e.g. yarn stock shortfall for an order). */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /** Words in the note that could not be placed anywhere. */
    @Builder.Default
    private List<String> unmatched = new ArrayList<>();

    private String originalText;

    /** Echoed from config so the frontend knows when to auto-submit. */
    private double autoSubmitThreshold;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Resolved {
        private String token;
        /** PARTY | PRODUCT | MATERIAL | ORDER | RESOURCE */
        private String kind;
        private Long id;
        private String name;
        private double score;
    }
}
