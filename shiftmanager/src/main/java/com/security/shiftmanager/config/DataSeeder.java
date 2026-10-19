package com.security.shiftmanager.config;

import com.security.shiftmanager.model.User;
import com.security.shiftmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator when the database has no users. The credentials come from
 * ADMIN_EMAIL / ADMIN_PASSWORD; the defaults are only meant for local development.
 */
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
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User(null, "admin", adminEmail, passwordEncoder.encode(adminPassword), true);
        userRepository.save(admin);
        if (DEV_PASSWORD.equals(adminPassword)) {
            log.info("Seeded dev admin user -> email: {}, password: {}", adminEmail, adminPassword);
        } else {
            log.info("Seeded admin user -> email: {}", adminEmail);
        }
    }
}
