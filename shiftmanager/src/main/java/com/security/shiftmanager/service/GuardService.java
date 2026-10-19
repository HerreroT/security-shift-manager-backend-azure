package com.security.shiftmanager.service;

import com.security.shiftmanager.dto.GuardRequest;
import com.security.shiftmanager.dto.GuardResponse;
import com.security.shiftmanager.model.Guard;
import com.security.shiftmanager.repository.GuardRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class GuardService {

    private final GuardRepository guardRepository;

    public GuardService(GuardRepository guardRepository) {
        this.guardRepository = guardRepository;
    }

    @Transactional(readOnly = true)
    public List<GuardResponse> search(String search, Boolean active) {
        return guardRepository.search(SearchTerms.toLikePattern(search), active).stream()
                .map(GuardResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GuardResponse findById(Long id) {
        return GuardResponse.from(getGuard(id));
    }

    public GuardResponse create(GuardRequest request) {
        ensureDniIsFree(request.dni(), null);
        Guard guard = new Guard();
        apply(guard, request);
        return GuardResponse.from(guardRepository.save(guard));
    }

    public GuardResponse update(Long id, GuardRequest request) {
        Guard guard = getGuard(id);
        ensureDniIsFree(request.dni(), id);
        apply(guard, request);
        return GuardResponse.from(guard);
    }

    public GuardResponse setActive(Long id, boolean active) {
        Guard guard = getGuard(id);
        guard.setActive(active);
        return GuardResponse.from(guard);
    }

    Guard getGuard(Long id) {
        return guardRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Guard not found with id " + id));
    }

    private void ensureDniIsFree(String dni, Long currentId) {
        guardRepository.findByDni(dni)
                .filter(existing -> !existing.getId().equals(currentId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Guard already exists with dni " + dni);
                });
    }

    private void apply(Guard guard, GuardRequest request) {
        guard.setFirstName(request.firstName().trim());
        guard.setLastName(request.lastName().trim());
        guard.setDni(request.dni());
        guard.setPhone(request.phone().trim());
        guard.setActive(request.active() == null || request.active());
    }
}
