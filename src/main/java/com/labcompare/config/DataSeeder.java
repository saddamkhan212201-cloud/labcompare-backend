package com.labcompare.config;

import com.labcompare.model.User;
import com.labcompare.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DataSeeder — seeds only system users on first boot.
 *
 * Labs, tests, and prices are NOT hardcoded here.
 * They must be created by admins via the Admin Panel.
 * This is a production application — all data is DB-driven.
 */
@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(UserRepository userRepo, PasswordEncoder passwordEncoder) {
        return args -> {

            // SuperAdmin — full access to all labs, tests, prices
            if (!userRepo.existsByUsername("superadmin")) {
                userRepo.save(new User("superadmin", passwordEncoder.encode("super123"), User.Role.SUPERADMIN));
                System.out.println("[LabCompare] SuperAdmin created: superadmin / super123");
            }

            // Generic admin — not restricted to a specific lab
            if (!userRepo.existsByUsername("admin")) {
                userRepo.save(new User("admin", passwordEncoder.encode("admin123"), User.Role.ADMIN));
                System.out.println("[LabCompare] Admin created: admin / admin123");
            }

            // Default user
            if (!userRepo.existsByUsername("user")) {
                userRepo.save(new User("user", passwordEncoder.encode("user123"), User.Role.USER));
                System.out.println("[LabCompare] Default user created: user / user123");
            }

            System.out.println("[LabCompare] Startup complete. Add labs and tests via the Admin Panel.");
        };
    }
}