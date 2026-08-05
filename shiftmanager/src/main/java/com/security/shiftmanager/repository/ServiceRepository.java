package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceRepository extends JpaRepository<Service, Long> {
}
