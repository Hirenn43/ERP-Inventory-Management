package com.erp.inventory.entity.enums;

/** One stage in an order's lifecycle. An order accumulates one OrderFulfillmentEvent per stage/quantity
 *  instead of carrying a single overwritten status - mirrors ordered -> completed -> dispatched -> settled,
 *  with RETURNED covering post-delivery returns. */
public enum FulfillmentStage {
    COMPLETED,
    DISPATCHED,
    SETTLED,
    RETURNED
}
