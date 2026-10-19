package com.security.shiftmanager.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record GuardHoursReport(
        GuardResponse guard,
        LocalDate from,
        LocalDate to,
        BigDecimal totalHours,
        int shiftCount,
        List<ShiftAssignmentResponse> shifts
) {
}
