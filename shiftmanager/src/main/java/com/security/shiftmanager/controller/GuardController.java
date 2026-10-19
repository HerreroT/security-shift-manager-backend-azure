package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.GuardRequest;
import com.security.shiftmanager.dto.GuardResponse;
import com.security.shiftmanager.service.GuardService;
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
@RequestMapping("/api/guards")
public class GuardController {

    private final GuardService guardService;

    public GuardController(GuardService guardService) {
        this.guardService = guardService;
    }

    @GetMapping
    public List<GuardResponse> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active) {
        return guardService.search(search, active);
    }

    @GetMapping("/{id}")
    public GuardResponse getById(@PathVariable Long id) {
        return guardService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GuardResponse create(@Valid @RequestBody GuardRequest request) {
        return guardService.create(request);
    }

    @PutMapping("/{id}")
    public GuardResponse update(@PathVariable Long id, @Valid @RequestBody GuardRequest request) {
        return guardService.update(id, request);
    }

    /** Soft delete: the guard is deactivated so its shift history is kept. */
    @DeleteMapping("/{id}")
    public GuardResponse deactivate(@PathVariable Long id) {
        return guardService.setActive(id, false);
    }

    @PatchMapping("/{id}/activate")
    public GuardResponse activate(@PathVariable Long id) {
        return guardService.setActive(id, true);
    }
}
