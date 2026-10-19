package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.ServiceRequest;
import com.security.shiftmanager.dto.ServiceResponse;
import com.security.shiftmanager.dto.ShiftAssignmentResponse;
import com.security.shiftmanager.service.ReportService;
import com.security.shiftmanager.service.ServiceService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/services")
public class ServiceController {

    private final ServiceService serviceService;
    private final ReportService reportService;

    public ServiceController(ServiceService serviceService, ReportService reportService) {
        this.serviceService = serviceService;
        this.reportService = reportService;
    }

    @GetMapping
    public List<ServiceResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return serviceService.search(search, active);
    }

    @GetMapping("/{id}")
    public ServiceResponse getById(@PathVariable Long id) {
        return serviceService.findById(id);
    }

    /** Who was working there at that moment. Example: GET /api/services/1/on-duty?at=2026-10-12T16:00 */
    @GetMapping("/{id}/on-duty")
    public List<ShiftAssignmentResponse> getOnDuty(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return reportService.onDuty(id, at);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceResponse create(@Valid @RequestBody ServiceRequest request) {
        return serviceService.create(request);
    }

    @PutMapping("/{id}")
    public ServiceResponse update(@PathVariable Long id, @Valid @RequestBody ServiceRequest request) {
        return serviceService.update(id, request);
    }

    /** Soft delete: the service is deactivated so its shift history is kept. */
    @DeleteMapping("/{id}")
    public ServiceResponse deactivate(@PathVariable Long id) {
        return serviceService.setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    public ServiceResponse activate(@PathVariable Long id) {
        return serviceService.setActive(id, true);
    }
}
