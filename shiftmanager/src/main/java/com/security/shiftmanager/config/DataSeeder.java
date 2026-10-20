package com.security.shiftmanager.config;

import com.security.shiftmanager.model.Role;
import com.security.shiftmanager.model.User;
import com.security.shiftmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator when the database has no administrator. The credentials come from
 * ADMIN_EMAIL / ADMIN_PASSWORD; the defaults are only meant for local development.
 */
@Order(1)
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEV_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email}") String adminEmail,
            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        // Checks for an ADMIN instead of "any user": if another seeder created a user first,
        // the application would otherwise end up without an administrator.
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        User admin = new User(null, "admin", adminEmail, passwordEncoder.encode(adminPassword), true, Role.ADMIN);
        userRepository.save(admin);
        if (DEV_PASSWORD.equals(adminPassword)) {
            // Never log the password itself; only warn that the public development default is in use.
            log.warn("Seeded admin user -> email: {} with the DEVELOPMENT default password (see README)", adminEmail);
        } else {
            log.info("Seeded admin user -> email: {}", adminEmail);
        }
    }
} 
//prueba subida
