package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.ShiftAssignmentRequest;
import com.security.shiftmanager.dto.ShiftAssignmentResponse;
import com.security.shiftmanager.service.ShiftAssignmentService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shift-assignments")
public class ShiftAssignmentController {

    private final ShiftAssignmentService shiftAssignmentService;

    public ShiftAssignmentController(ShiftAssignmentService shiftAssignmentService) {
        this.shiftAssignmentService = shiftAssignmentService;
    }

    @GetMapping
    public List<ShiftAssignmentResponse> getAll(
            @RequestParam(required = false) Long guardId,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return shiftAssignmentService.search(guardId, serviceId, from, to);
    }

    @GetMapping("/{id}")
    public ShiftAssignmentResponse getById(@PathVariable Long id) {
        return shiftAssignmentService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ShiftAssignmentResponse create(@Valid @RequestBody ShiftAssignmentRequest request) {
        return shiftAssignmentService.create(request);
    }

    @PutMapping("/{id}")
    public ShiftAssignmentResponse update(
            @PathVariable Long id, @Valid @RequestBody ShiftAssignmentRequest request) {
        return shiftAssignmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        shiftAssignmentService.delete(id);
    }

    @PostMapping("/{id}/check-in")
    public ShiftAssignmentResponse checkIn(@PathVariable Long id) {
        return shiftAssignmentService.checkIn(id);
    }

    @PostMapping("/{id}/check-out")
    public ShiftAssignmentResponse checkOut(@PathVariable Long id) {
        return shiftAssignmentService.checkOut(id);
    }
}
