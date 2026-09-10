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
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            User admin = User.builder()
                    .fullName("System Administrator")
                    .username("admin")
                    .email("admin@inventory.local")
                    .password(passwordEncoder.encode("Admin@123"))
                    .roles(Set.of(adminRole))
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
    }
}
