package com.erp.inventory.entity.enums;

public enum PartyRole {
    CUSTOMER,
    SUPPLIER,
    MANUFACTURER,
    /** A third-party processor who receives material/product, processes it (dyeing, printing,
     *  embroidery...), and returns it - see JobWorkOrder. */
    JOB_WORKER
}
