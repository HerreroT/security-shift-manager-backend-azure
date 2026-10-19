package com.security.shiftmanager.service;

import com.security.shiftmanager.model.ShiftAssignment;
import com.security.shiftmanager.repository.ShiftAssignmentRepository;
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
        shiftAssignment.setActualEndTime(LocalDateTime.now());
        shiftAssignment.recalculateTotalHours();
        return shiftAssignmentRepository.save(shiftAssignment);
    }
}
