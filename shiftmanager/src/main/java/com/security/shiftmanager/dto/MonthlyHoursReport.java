package com.security.shiftmanager.dto;

import java.math.BigDecimal;
import java.util.List;

public record MonthlyHoursReport(
        String month,
        BigDecimal totalHours,
        int shiftCount,
        List<Row> guards
) {

    public record Row(
            Long guardId,
            String firstName,
            String lastName,
            String dni,
            boolean active,
            int shiftCount,
            BigDecimal totalHours
    ) {
    }
}
