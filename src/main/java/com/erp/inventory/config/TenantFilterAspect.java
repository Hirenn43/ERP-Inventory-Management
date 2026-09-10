package com.erp.inventory.config;

import com.erp.inventory.security.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;
import org.springframework.stereotype.Component;

/**
 * Switches on the Hibernate {@code tenantFilter} for the current request, using the company id
 * that {@code JwtAuthenticationFilter} (REST) or {@code QuickEntryExecutor} (WhatsApp) put in
 * {@link TenantContext}.
 *
 * <p>Runs before every {@code com.erp.inventory.service} call. With {@code open-in-view=true}
 * the request already has a bound {@code EntityManager}, so the filter is active for the whole
 * unit of work — list queries, derived {@code existsBy…} checks, and lazy association loads all
 * see only the caller's tenant. {@code find(id)} bypasses Hibernate filters, so id lookups go
 * through {@code findOneById} JPQL queries in the repositories instead.
 *
 * <p>When {@link TenantContext} is empty (auth endpoints, the seeder, tests) nothing is enabled
 * and every query behaves exactly as before.
 */
@Aspect
@Component
@RequiredArgsConstructor
public class TenantFilterAspect {

    private final EntityManagerFactory entityManagerFactory;

    @Before("execution(* com.erp.inventory.service..*(..))")
    public void enableTenantFilter() {
        Long companyId = TenantContext.get();
        if (companyId == null) {
            return;
        }
        EntityManager em =
                EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (em == null) {
            return;
        }
        Session session = em.unwrap(Session.class);
        if (session.getEnabledFilter("tenantFilter") == null) {
            session.enableFilter("tenantFilter").setParameter("companyId", companyId);
        }
    }
}
