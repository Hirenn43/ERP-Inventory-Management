package com.erp.inventory.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Builds a full request DTO for a PATCH: start from the resource's current state, overlay only
 * the fields present in the patch body (an explicit {@code null} clears; an absent field is
 * kept), then bean-validate the result. The service's existing {@code update(id, request)} then
 * applies it, so PATCH and PUT share one write path.
 */
@Component
@RequiredArgsConstructor
public class PatchMerger {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public <REQ> REQ merge(Object currentResponse, Class<REQ> requestType, JsonNode patch) {
        REQ base = objectMapper.convertValue(currentResponse, requestType);
        try {
            REQ merged = objectMapper.readerForUpdating(base).readValue(patch);
            Set<ConstraintViolation<REQ>> violations = validator.validate(merged);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
            return merged;
        } catch (IOException e) {
            throw new IllegalArgumentException("Malformed PATCH body");
        }
    }
}
