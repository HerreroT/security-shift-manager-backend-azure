package com.security.shiftmanager.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

public final class HoursCalculator {

    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private HoursCalculator() {
    }

    public static BigDecimal between(LocalDateTime start, LocalDateTime end) {
        BigDecimal minutes = BigDecimal.valueOf(Duration.between(start, end).toMinutes());
        return minutes.divide(MINUTES_PER_HOUR, 2, RoundingMode.HALF_UP);
    }
}
