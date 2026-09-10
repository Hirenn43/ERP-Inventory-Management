package com.erp.inventory.exception;

import java.util.List;

/** Raised when a resource's {@code attributes} bag violates its tenant custom-field rules. */
public class AttributeValidationException extends RuntimeException {

    private final List<String> details;

    public AttributeValidationException(List<String> details) {
        super("Custom field validation failed");
        this.details = details;
    }

    public List<String> getDetails() {
        return details;
    }
}
