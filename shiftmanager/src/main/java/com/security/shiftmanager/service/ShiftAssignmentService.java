package com.security.shiftmanager.service;

import com.security.shiftmanager.dto.ShiftAssignmentRequest;
import com.security.shiftmanager.dto.ShiftAssignmentResponse;
import com.security.shiftmanager.model.Guard;
import com.security.shiftmanager.model.Service;
import com.security.shiftmanager.model.ShiftAssignment;
import com.security.shiftmanager.repository.ShiftAssignmentRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@org.springframework.stereotype.Service
@Transactional
public class ShiftAssignmentService {

    private static final Duration MAX_SHIFT_DURATION = Duration.ofHours(24);
    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final GuardService guardService;
    private final ServiceService serviceService;

    public ShiftAssignmentService(
            ShiftAssignmentRepository shiftAssignmentRepository,
            GuardService guardService,
            ServiceService serviceService) {
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.guardService = guardService;
        this.serviceService = serviceService;
    }

    /**
     * Lists shifts that overlap the given days (both inclusive). A night shift that starts the day
     * before "from" and ends on "from" is included.
     */
    @Transactional(readOnly = true)
    public List<ShiftAssignmentResponse> search(Long guardId, Long serviceId, LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must not be before from");
        }
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();
        return shiftAssignmentRepository.search(guardId, serviceId, fromDateTime, toDateTime).stream()
                .map(ShiftAssignmentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftAssignmentResponse findById(Long id) {
        return ShiftAssignmentResponse.from(getShift(id));
    }

    public ShiftAssignmentResponse create(ShiftAssignmentRequest request) {
        Guard guard = activeGuard(request.guardId());
        Service service = activeService(request.serviceId());
        validateTimes(request);
        ensureNoOverlap(guard.getId(), request.startDateTime(), request.endDateTime(), null);

        ShiftAssignment shift = new ShiftAssignment();
        shift.setGuard(guard);
        shift.setService(service);
        apply(shift, request);
        return ShiftAssignmentResponse.from(shiftAssignmentRepository.save(shift));
    }

    public ShiftAssignmentResponse update(Long id, ShiftAssignmentRequest request) {
        ShiftAssignment shift = getShift(id);
        // Inactive guards/services keep their history, but nothing new can be assigned to them.
        if (!shift.getGuard().getId().equals(request.guardId())) {
            shift.setGuard(activeGuard(request.guardId()));
        }
        if (!shift.getService().getId().equals(request.serviceId())) {
            shift.setService(activeService(request.serviceId()));
        }
        validateTimes(request);
        ensureNoOverlap(request.guardId(), request.startDateTime(), request.endDateTime(), id);
        apply(shift, request);
        return ShiftAssignmentResponse.from(shift);
    }

    public void delete(Long id) {
        shiftAssignmentRepository.delete(getShift(id));
    }

    public ShiftAssignmentResponse checkIn(Long id) {
        ShiftAssignment shiftAssignment = getShift(id);
        if (shiftAssignment.getActualStartTime() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "ShiftAssignment " + id + " already has a check-in");
        }
        shiftAssignment.setActualStartTime(LocalDateTime.now());
        return ShiftAssignmentResponse.from(shiftAssignment);
    }

    public ShiftAssignmentResponse checkOut(Long id) {
        ShiftAssignment shiftAssignment = getShift(id);
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
        return ShiftAssignmentResponse.from(shiftAssignment);
    }

    private ShiftAssignment getShift(Long id) {
        return shiftAssignmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ShiftAssignment not found with id " + id));
    }

    private Guard activeGuard(Long guardId) {
        Guard guard = guardService.getGuard(guardId);
        if (!guard.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Guard " + guardId + " is inactive");
        }
        return guard;
    }

    private Service activeService(Long serviceId) {
        Service service = serviceService.getService(serviceId);
        if (!service.isActive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Service " + serviceId + " is inactive");
        }
        return service;
    }

    private void validateTimes(ShiftAssignmentRequest request) {
        if (!request.endDateTime().isAfter(request.startDateTime())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "endDateTime must be after startDateTime");
        }
        if (Duration.between(request.startDateTime(), request.endDateTime()).compareTo(MAX_SHIFT_DURATION) > 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A shift cannot be longer than 24 hours");
        }
    }

    private void ensureNoOverlap(Long guardId, LocalDateTime start, LocalDateTime end, Long excludeId) {
        List<ShiftAssignment> overlapping = shiftAssignmentRepository.findOverlapping(guardId, start, end, excludeId);
        if (!overlapping.isEmpty()) {
            ShiftAssignment conflict = overlapping.get(0);
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(
                    "Guard already has shift %d at %s from %s to %s",
                    conflict.getId(),
                    conflict.getService().getName(),
                    conflict.getStartDateTime().format(DISPLAY_FORMAT),
                    conflict.getEndDateTime().format(DISPLAY_FORMAT)));
        }
    }

    private void apply(ShiftAssignment shift, ShiftAssignmentRequest request) {
        shift.setStartDateTime(request.startDateTime());
        shift.setEndDateTime(request.endDateTime());
        shift.setNotes(request.notes());
        shift.recalculateTotalHours();
    }
}
