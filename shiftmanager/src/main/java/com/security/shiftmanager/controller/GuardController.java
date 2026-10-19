package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.GuardHoursReport;
import com.security.shiftmanager.dto.GuardRequest;
import com.security.shiftmanager.dto.GuardResponse;
import com.security.shiftmanager.service.GuardService;
import com.security.shiftmanager.service.ReportService;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
@RequestMapping("/api/guards")
public class GuardController {

    private final GuardService guardService;
    private final ReportService reportService;

    public GuardController(GuardService guardService, ReportService reportService) {
        this.guardService = guardService;
        this.reportService = reportService;
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

    /** Hours worked by the guard. Example: GET /api/guards/1/hours?from=2026-10-01&to=2026-10-31 */
    @GetMapping("/{id}/hours")
    public GuardHoursReport getHours(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reportService.guardHours(id, from, to);
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
