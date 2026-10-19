package com.security.shiftmanager.service;

import com.security.shiftmanager.dto.GuardHoursReport;
import com.security.shiftmanager.dto.GuardResponse;
import com.security.shiftmanager.dto.MonthlyHoursReport;
import com.security.shiftmanager.dto.ShiftAssignmentResponse;
import com.security.shiftmanager.model.Guard;
import com.security.shiftmanager.model.ShiftAssignment;
import com.security.shiftmanager.repository.GuardRepository;
import com.security.shiftmanager.repository.ShiftAssignmentRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final ShiftAssignmentRepository shiftAssignmentRepository;
    private final GuardRepository guardRepository;
    private final GuardService guardService;
    private final ServiceService serviceService;

    public ReportService(
            ShiftAssignmentRepository shiftAssignmentRepository,
            GuardRepository guardRepository,
            GuardService guardService,
            ServiceService serviceService) {
        this.shiftAssignmentRepository = shiftAssignmentRepository;
        this.guardRepository = guardRepository;
        this.guardService = guardService;
        this.serviceService = serviceService;
    }

    /** Who was working at a service at a given date and time. */
    public List<ShiftAssignmentResponse> onDuty(Long serviceId, LocalDateTime at) {
        serviceService.getService(serviceId);
        return shiftAssignmentRepository.findOnDuty(serviceId, at).stream()
                .map(ShiftAssignmentResponse::from)
                .toList();
    }

    /**
     * Hours worked by a guard between two days (both inclusive). A shift is counted in the day it
     * starts. Defaults to the current month when the dates are missing.
     */
    public GuardHoursReport guardHours(Long guardId, LocalDate from, LocalDate to) {
        Guard guard = guardService.getGuard(guardId);
        YearMonth currentMonth = YearMonth.now();
        LocalDate start = from != null ? from : currentMonth.atDay(1);
        LocalDate end = to != null ? to : currentMonth.atEndOfMonth();
        if (end.isBefore(start)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to must not be before from");
        }

        List<ShiftAssignment> shifts = shiftAssignmentRepository.findStartingBetween(
                guardId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return new GuardHoursReport(
                GuardResponse.from(guard),
                start,
                end,
                sumHours(shifts),
                shifts.size(),
                shifts.stream().map(ShiftAssignmentResponse::from).toList());
    }

    /**
     * Total hours per guard for a month (format yyyy-MM). Includes every active guard, even with 0
     * hours, and inactive guards that worked that month.
     */
    public MonthlyHoursReport monthlyHours(String month) {
        YearMonth yearMonth = parseMonth(month);
        List<ShiftAssignment> shifts = shiftAssignmentRepository.findStartingBetween(
                null, yearMonth.atDay(1).atStartOfDay(), yearMonth.plusMonths(1).atDay(1).atStartOfDay());
        Map<Long, List<ShiftAssignment>> shiftsByGuard = shifts.stream()
                .collect(Collectors.groupingBy(shift -> shift.getGuard().getId()));

        List<MonthlyHoursReport.Row> rows = guardRepository.findAll().stream()
                .filter(guard -> guard.isActive() || shiftsByGuard.containsKey(guard.getId()))
                .sorted(Comparator.comparing(Guard::getLastName).thenComparing(Guard::getFirstName))
                .map(guard -> {
                    List<ShiftAssignment> guardShifts = shiftsByGuard.getOrDefault(guard.getId(), List.of());
                    return new MonthlyHoursReport.Row(
                            guard.getId(),
                            guard.getFirstName(),
                            guard.getLastName(),
                            guard.getDni(),
                            guard.isActive(),
                            guardShifts.size(),
                            sumHours(guardShifts));
                })
                .toList();

        return new MonthlyHoursReport(yearMonth.toString(), sumHours(shifts), shifts.size(), rows);
    }

    /** Monthly report as CSV, ready to open in Excel (semicolon separated, UTF-8 with BOM). */
    public String monthlyHoursCsv(String month) {
        MonthlyHoursReport report = monthlyHours(month);
        StringBuilder csv = new StringBuilder("﻿");
        csv.append("Apellido;Nombre;DNI;Activo;Turnos;Horas\n");
        for (MonthlyHoursReport.Row row : report.guards()) {
            csv.append(csvValue(row.lastName())).append(';')
                    .append(csvValue(row.firstName())).append(';')
                    .append(row.dni()).append(';')
                    .append(row.active() ? "Si" : "No").append(';')
                    .append(row.shiftCount()).append(';')
                    .append(row.totalHours().toPlainString().replace('.', ','))
                    .append('\n');
        }
        return csv.toString();
    }

    private static YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "month must have the format yyyy-MM");
        }
    }

    private static BigDecimal sumHours(List<ShiftAssignment> shifts) {
        return shifts.stream()
                .map(ShiftAssignment::getTotalHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
    }

    private static String csvValue(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
