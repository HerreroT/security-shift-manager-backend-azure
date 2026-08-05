package com.security.shiftmanager.controller;

import com.security.shiftmanager.model.ShiftAssignment;
import com.security.shiftmanager.service.ShiftAssignmentService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shift-assignments")
public class ShiftAssignmentController {

    private final ShiftAssignmentService shiftAssignmentService;

    public ShiftAssignmentController(ShiftAssignmentService shiftAssignmentService) {
        this.shiftAssignmentService = shiftAssignmentService;
    }

    @PostMapping("/{id}/check-in")
    public ShiftAssignment checkIn(@PathVariable Long id) {
        return shiftAssignmentService.checkIn(id);
    }

    @PostMapping("/{id}/check-out")
    public ShiftAssignment checkOut(@PathVariable Long id) {
        return shiftAssignmentService.checkOut(id);
    }

    @GetMapping("/{id}")
    public ShiftAssignment getById(@PathVariable Long id) {
        return shiftAssignmentService.findById(id);
    }

    @GetMapping("/guard/{guardId}")
    public List<ShiftAssignment> getByGuardId(@PathVariable Long guardId) {
        return shiftAssignmentService.findByGuardId(guardId);
    }
}
