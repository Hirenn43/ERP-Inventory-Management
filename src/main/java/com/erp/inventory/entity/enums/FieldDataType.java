package com.erp.inventory.entity.enums;

/** The kind of value a custom field holds — drives the input widget and validation. */
public enum FieldDataType {
    TEXT,
    NUMBER,
    INTEGER,
    BOOLEAN,
    DATE,
    /** One value from a fixed {@code options} list. */
    SELECT
}
