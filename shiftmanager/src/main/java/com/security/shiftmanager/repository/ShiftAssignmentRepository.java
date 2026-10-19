package com.security.shiftmanager.repository;

import com.security.shiftmanager.model.ShiftAssignment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShiftAssignmentRepository extends JpaRepository<ShiftAssignment, Long> {

    List<ShiftAssignment> findByGuardId(Long guardId);

    List<ShiftAssignment> findByServiceId(Long serviceId);

    /**
     * Shifts that overlap the [from, to) window. Every filter is optional.
     */
    @Query("""
            select s from ShiftAssignment s
            join fetch s.guard
            join fetch s.service
            where (:guardId is null or s.guard.id = :guardId)
              and (:serviceId is null or s.service.id = :serviceId)
              and (cast(:from as LocalDateTime) is null or s.endDateTime > :from)
              and (cast(:to as LocalDateTime) is null or s.startDateTime < :to)
            order by s.startDateTime
            """)
    List<ShiftAssignment> search(
            @Param("guardId") Long guardId,
            @Param("serviceId") Long serviceId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Shifts of a guard that overlap [start, end). Back-to-back shifts do not overlap.
     */
    @Query("""
            select s from ShiftAssignment s
            join fetch s.service
            where s.guard.id = :guardId
              and s.startDateTime < :end
              and s.endDateTime > :start
              and (:excludeId is null or s.id <> :excludeId)
            order by s.startDateTime
            """)
    List<ShiftAssignment> findOverlapping(
            @Param("guardId") Long guardId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);
}
