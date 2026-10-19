package com.security.shiftmanager.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shift_assignments", indexes = {
        @Index(name = "idx_shift_guard_start", columnList = "guard_id, startDateTime"),
        @Index(name = "idx_shift_service_start", columnList = "service_id, startDateTime")
})
public class ShiftAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guard_id", nullable = false)
    private Guard guard;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal totalHours;

    @Column(length = 500)
    private String notes;

    @Column
    private LocalDateTime actualStartTime;

    @Column
    private LocalDateTime actualEndTime;

    public ShiftAssignment() {
    }

    public ShiftAssignment(
            Long id,
            Guard guard,
            Service service,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            BigDecimal totalHours,
            String notes
    ) {
        this.id = id;
        this.guard = guard;
        this.service = service;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.totalHours = totalHours;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Guard getGuard() {
        return guard;
    }

    public void setGuard(Guard guard) {
        this.guard = guard;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public BigDecimal getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(BigDecimal totalHours) {
        this.totalHours = totalHours;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getActualStartTime() {
        return actualStartTime;
    }

    public void setActualStartTime(LocalDateTime actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public LocalDateTime getActualEndTime() {
        return actualEndTime;
    }

    public void setActualEndTime(LocalDateTime actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    /**
     * Recalculates totalHours: real worked time if the guard checked in and out,
     * otherwise the planned shift duration.
     */
    public void recalculateTotalHours() {
        if (actualStartTime != null && actualEndTime != null) {
            this.totalHours = HoursCalculator.between(actualStartTime, actualEndTime);
        } else {
            this.totalHours = HoursCalculator.between(startDateTime, endDateTime);
        }
    }
}
