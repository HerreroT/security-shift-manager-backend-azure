package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.Role;
import com.security.shiftmanager.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    boolean existsByRole(Role role);
}
