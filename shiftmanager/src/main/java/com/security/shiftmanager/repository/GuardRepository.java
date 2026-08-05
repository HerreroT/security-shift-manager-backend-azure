package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.Guard;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardRepository extends JpaRepository<Guard, Long> {

    Optional<Guard> findByDni(String dni);
}
