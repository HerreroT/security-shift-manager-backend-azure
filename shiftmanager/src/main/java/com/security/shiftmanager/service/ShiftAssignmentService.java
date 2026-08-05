package com.security.shiftmanager.service;

import com.security.shiftmanager.model.ShiftAssignment;
import com.security.shiftmanager.repository.ShiftAssignmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ShiftAssignmentService {

    private final ShiftAssignmentRepository shiftAssignmentRepository;

    public ShiftAssignmentService(ShiftAssignmentRepository shiftAssignmentRepository) {
        this.shiftAssignmentRepository = shiftAssignmentRepository;
    }

    public ShiftAssignment findById(Long id) {
        return shiftAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ShiftAssignment not found with id " + id));
    }

    public List<ShiftAssignment> findByGuardId(Long guardId) {
        return shiftAssignmentRepository.findByGuardId(guardId);
    }

    public ShiftAssignment checkIn(Long id) {
        ShiftAssignment shiftAssignment = findById(id);
        if (shiftAssignment.getActualStartTime() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ShiftAssignment " + id + " already has a check-in");
        }
        shiftAssignment.setActualStartTime(LocalDateTime.now());
        return shiftAssignmentRepository.save(shiftAssignment);
    }

    public ShiftAssignment checkOut(Long id) {
        ShiftAssignment shiftAssignment = findById(id);
        if (shiftAssignment.getActualStartTime() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ShiftAssignment " + id + " has no check-in registered");
        }
        if (shiftAssignment.getActualEndTime() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ShiftAssignment " + id + " already has a check-out");
        }
        LocalDateTime now = LocalDateTime.now();
        shiftAssignment.setActualEndTime(now);
        shiftAssignment.setTotalHours(calculateTotalHours(shiftAssignment.getActualStartTime(), now));
        return shiftAssignmentRepository.save(shiftAssignment);
    }

    private BigDecimal calculateTotalHours(LocalDateTime start, LocalDateTime end) {
        BigDecimal minutes = BigDecimal.valueOf(Duration.between(start, end).toMinutes());
        return minutes.divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
