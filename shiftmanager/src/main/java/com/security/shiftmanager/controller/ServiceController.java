package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.ServiceRequest;
import com.security.shiftmanager.dto.ServiceResponse;
import com.security.shiftmanager.service.ServiceService;
import jakarta.validation.Valid;
import java.util.List;
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

    public ServiceController(ServiceService serviceService) {
        this.serviceService = serviceService;
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
