package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.ShiftAssignment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByGuardId(Long guardId);

    List<ShiftAssignment> findByServiceId(Long serviceId);
}
