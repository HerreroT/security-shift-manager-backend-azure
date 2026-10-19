package com.security.shiftmanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class HoursCalculatorTest {

    @Test
    void nightShiftCrossingMidnightIsTwelveHours() {
        BigDecimal hours = HoursCalculator.between(
                LocalDateTime.of(2026, 10, 12, 20, 0),
                LocalDateTime.of(2026, 10, 13, 8, 0));

        assertThat(hours).isEqualByComparingTo("12.00");
    }

    @Test
    void partialHoursAreRoundedToTwoDecimals() {
        BigDecimal hours = HoursCalculator.between(
                LocalDateTime.of(2026, 10, 12, 8, 0),
                LocalDateTime.of(2026, 10, 12, 14, 30));

        assertThat(hours).isEqualByComparingTo("6.50");
    }

    @Test
    void recalculateUsesActualTimesWhenCheckedOut() {
        ShiftAssignment shift = new ShiftAssignment();
        shift.setStartDateTime(LocalDateTime.of(2026, 10, 12, 8, 0));
        shift.setEndDateTime(LocalDateTime.of(2026, 10, 12, 20, 0));
        shift.recalculateTotalHours();
        assertThat(shift.getTotalHours()).isEqualByComparingTo("12.00");

        shift.setActualStartTime(LocalDateTime.of(2026, 10, 12, 8, 15));
        shift.setActualEndTime(LocalDateTime.of(2026, 10, 12, 20, 0));
        shift.recalculateTotalHours();
        assertThat(shift.getTotalHours()).isEqualByComparingTo("11.75");
    }
}
