package com.erp.inventory.entity.enums;

/** What a quick-note line was understood to mean. Not persisted - only used by the
 *  /api/quick-entry/parse response. */
public enum QuickEntryAction {
    CREATE_ORDER,
    RECORD_YARN_PURCHASE,
    ADD_FULFILLMENT_EVENT,
    CREATE_PARTY,
    CREATE_PRODUCT,
    CREATE_BROKER,
    CREATE_AREA,
    CREATE_SCHEDULE,
    CREATE_JOB,
    CREATE_DESIGN,
    CREATE_TRANSPORTER,
    CREATE_VEHICLE,
    UNKNOWN
}
