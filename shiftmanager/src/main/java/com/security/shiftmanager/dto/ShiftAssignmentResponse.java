package com.security.shiftmanager.dto;

import com.security.shiftmanager.model.ShiftAssignment;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShiftAssignmentResponse(
        Long id,
        GuardResponse guard,
        ServiceResponse service,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        BigDecimal totalHours,
        String notes,
        LocalDateTime actualStartTime,
        LocalDateTime actualEndTime
) {

    public static ShiftAssignmentResponse from(ShiftAssignment shift) {
        return new ShiftAssignmentResponse(
                shift.getId(),
                GuardResponse.from(shift.getGuard()),
                ServiceResponse.from(shift.getService()),
                shift.getStartDateTime(),
                shift.getEndDateTime(),
                shift.getTotalHours(),
                shift.getNotes(),
                shift.getActualStartTime(),
                shift.getActualEndTime());
    }
}
