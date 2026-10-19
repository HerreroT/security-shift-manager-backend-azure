package com.security.shiftmanager.service;

import com.security.shiftmanager.model.Guard;
import com.security.shiftmanager.repository.GuardRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GuardService {

    private final GuardRepository guardRepository;

    public GuardService(GuardRepository guardRepository) {
        this.guardRepository = guardRepository;
    }

    public List<Guard> findAll() {
        return guardRepository.findAll();
    }

    public Guard findById(Long id) {
        return guardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guard not found with id " + id));
    }

    public Guard create(Guard guard) {
        guardRepository.findByDni(guard.getDni()).ifPresent(existing -> {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Guard already exists with dni " + guard.getDni());
        });
        guard.setId(null);
        return guardRepository.save(guard);
    }

    public Guard update(Long id, Guard updated) {
        Guard guard = findById(id);
        guard.setFirstName(updated.getFirstName());
        guard.setLastName(updated.getLastName());
        guard.setDni(updated.getDni());
        guard.setPhone(updated.getPhone());
        guard.setActive(updated.isActive());
        return guardRepository.save(guard);
    }

    public Guard deactivate(Long id) {
        Guard guard = findById(id);
        guard.setActive(false);
        return guardRepository.save(guard);
    }
}
