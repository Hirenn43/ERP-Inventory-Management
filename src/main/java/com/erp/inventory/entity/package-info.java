/**
 * JPA entities.
 *
 * <p>{@link org.hibernate.annotations.FilterDef tenantFilter} is declared here once and
 * applied per-entity with {@code @Filter}. It is switched on for the current request by
 * {@code com.erp.inventory.config.TenantFilterAspect}; when it is not enabled (auth
 * endpoints, tests, non-tenant entities) every query behaves exactly as before.
 */
@FilterDef(
        name = "tenantFilter",
        parameters = @ParamDef(name = "companyId", type = Long.class),
        defaultCondition = "company_id = :companyId"
)
package com.erp.inventory.entity;

import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;
