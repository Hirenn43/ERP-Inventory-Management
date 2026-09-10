package com.erp.inventory.config;

import com.erp.inventory.entity.Company;
import com.erp.inventory.entity.Role;
import com.erp.inventory.entity.User;
import com.erp.inventory.entity.enums.RoleName;
import com.erp.inventory.repository.CompanyRepository;
import com.erp.inventory.repository.RoleRepository;
import com.erp.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Profile("!prod")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        for (RoleName roleName : RoleName.values()) {
            roleRepository.findByName(roleName)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
        }

        // A tenant root the seeded admin belongs to (multi-tenant: every user has a company).
        Company defaultCompany = companyRepository.findAll().stream().findFirst()
                .orElseGet(() -> companyRepository.save(
                        Company.builder().name("Default Company").build()));

        if (!userRepository.existsByUsername("admin")) {
            // The seeded admin holds every admin role so all of Support is reachable in dev.
            Set<Role> allAdminRoles = new java.util.HashSet<>();
            for (RoleName rn : new RoleName[] {
                    RoleName.ROLE_ADMIN, RoleName.ROLE_USER_ADMIN, RoleName.ROLE_MASTER_ADMIN }) {
                roleRepository.findByName(rn).ifPresent(allAdminRoles::add);
            }
            User admin = User.builder()
                    .fullName("System Administrator")
                    .username("admin")
                    .email("admin@inventory.local")
                    .password(passwordEncoder.encode("Admin@123"))
                    .roles(allAdminRoles)
                    .company(defaultCompany)
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("Seeded default admin user (username: admin) for non-prod profile. "
                    + "See project docs for the default password; change it before any shared deployment.");
        }

        // Backfill any pre-multi-tenancy user that has no company yet.
        userRepository.findAll().stream()
                .filter(u -> u.getCompany() == null)
                .forEach(u -> {
                    u.setCompany(defaultCompany);
                    userRepository.save(u);
                });

        // Ensure the dev admin holds every admin role (covers admins seeded before the Support module).
        userRepository.findByUsername("admin").ifPresent(admin -> {
            boolean changed = false;
            for (RoleName rn : new RoleName[] { RoleName.ROLE_USER_ADMIN, RoleName.ROLE_MASTER_ADMIN }) {
                boolean has = admin.getRoles().stream().anyMatch(r -> r.getName() == rn);
                if (!has) {
                    roleRepository.findByName(rn).ifPresent(r -> admin.getRoles().add(r));
                    changed = true;
                }
            }
            if (changed) {
                userRepository.save(admin);
            }
        });
    }
}
