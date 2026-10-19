package com.security.shiftmanager.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record ShiftAssignmentRequest(
        @NotNull Long guardId,
        @NotNull Long serviceId,
        @NotNull LocalDateTime startDateTime,
        @NotNull LocalDateTime endDateTime,
        @Size(max = 500) String notes
) {
}
