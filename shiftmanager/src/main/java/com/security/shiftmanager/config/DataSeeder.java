package com.security.shiftmanager.config;

import com.security.shiftmanager.model.User;
import com.security.shiftmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String SEED_EMAIL = "admin@shiftmanager.com";
    private static final String SEED_PASSWORD = "admin123";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        User admin = new User(null, "admin", SEED_EMAIL, passwordEncoder.encode(SEED_PASSWORD), true);
        userRepository.save(admin);
        log.info("Seeded dev admin user -> email: {}, password: {}", SEED_EMAIL, SEED_PASSWORD);
    }
}
