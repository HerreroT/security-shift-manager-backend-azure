package com.security.shiftmanager;

import static org.assertj.core.api.Assertions.assertThat;

import com.security.shiftmanager.config.DataSeeder;
import com.security.shiftmanager.model.Role;
import com.security.shiftmanager.model.User;
import com.security.shiftmanager.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reproduces the first Azure deploy: the demo seeder ran first and created the supervisor,
 * so a "no users" check skipped the administrator.
 */
@SpringBootTest(properties = "app.seed-demo-data=false")
@Transactional
class DataSeederTest {

    @Autowired
    private DataSeeder dataSeeder;

    @Autowired
    private UserRepository userRepository;

    @Test
    void createsTheAdministratorWhenOnlyOtherUsersExist() {
        userRepository.deleteAll();
        userRepository.save(new User(null, "supervisor", "supervisor@shiftmanager.com", "hash", true, Role.SUPERVISOR));

        dataSeeder.run();

        assertThat(userRepository.existsByRole(Role.ADMIN)).isTrue();
        assertThat(userRepository.findByEmail("admin@shiftmanager.com")).isPresent();
    }

    @Test
    void doesNotCreateASecondAdministrator() {
        long before = userRepository.count();

        dataSeeder.run();

        assertThat(userRepository.count()).isEqualTo(before);
    }
}
