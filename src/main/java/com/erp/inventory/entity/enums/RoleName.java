package com.erp.inventory.entity.enums;

public enum RoleName {
    ROLE_ADMIN,
    ROLE_MANAGER,
    ROLE_STAFF,
    /** Can manage the tenant's users and their role assignments (Support → User Management). */
    ROLE_USER_ADMIN,
    /** Can define the tenant's custom-field variables (Support → Master Management). */
    ROLE_MASTER_ADMIN
}
