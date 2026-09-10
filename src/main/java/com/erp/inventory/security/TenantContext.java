package com.erp.inventory.security;

/**
 * Holds the current request's tenant (company) id for the duration of the request thread.
 *
 * <p>Populated by {@code JwtAuthenticationFilter} for REST calls and by
 * {@code QuickEntryExecutor} for the WhatsApp ingress path, then read by
 * {@code TenantFilterAspect} to switch on the Hibernate {@code tenantFilter}. Always cleared
 * in a {@code finally} block so a pooled thread never leaks a tenant into the next request.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long companyId) {
        CURRENT.set(companyId);
    }

    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
