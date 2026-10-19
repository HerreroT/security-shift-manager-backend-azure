package com.security.shiftmanager.controller;

import com.security.shiftmanager.dto.MonthlyHoursReport;
import com.security.shiftmanager.service.ReportService;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /** Example: GET /api/reports/monthly-hours?month=2026-10 */
    @GetMapping("/monthly-hours")
    public MonthlyHoursReport monthlyHours(@RequestParam(required = false) String month) {
        return reportService.monthlyHours(month);
    }

    @GetMapping("/monthly-hours.csv")
    public ResponseEntity<byte[]> monthlyHoursCsv(@RequestParam(required = false) String month) {
        String csv = reportService.monthlyHoursCsv(month);
        String fileName = "horas-" + (month == null || month.isBlank() ? YearMonth.now() : month) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName).build().toString())
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
