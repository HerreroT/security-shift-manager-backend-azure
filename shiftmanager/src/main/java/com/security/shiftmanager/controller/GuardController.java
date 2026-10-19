package com.security.shiftmanager.controller;

import com.security.shiftmanager.model.Guard;
import com.security.shiftmanager.service.GuardService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/guards")
public class GuardController {

    private final GuardService guardService;

    public GuardController(GuardService guardService) {
        this.guardService = guardService;
    }

    @GetMapping
    public List<Guard> getAll() {
        return guardService.findAll();
    }

    @GetMapping("/{id}")
    public Guard getById(@PathVariable Long id) {
        return guardService.findById(id);
    }

    @PostMapping
    public Guard create(@RequestBody Guard guard) {
        return guardService.create(guard);
    }

    @PutMapping("/{id}")
    public Guard update(@PathVariable Long id, @RequestBody Guard guard) {
        return guardService.update(id, guard);
    }

    @PostMapping("/{id}/deactivate")
    public Guard deactivate(@PathVariable Long id) {
        return guardService.deactivate(id);
    }
}
