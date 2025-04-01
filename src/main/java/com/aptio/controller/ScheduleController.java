package com.aptio.controller;

import com.aptio.dto.ResponseScheduleEntryDTO;
import com.aptio.dto.ScheduleEntryDTO;
import com.aptio.model.BusinessSettings;
import com.aptio.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/schedule")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping("/staff/{staffId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<List<ResponseScheduleEntryDTO>> getStaffSchedule(
            @PathVariable String staffId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(scheduleService.getStaffSchedule(staffId, startDate, endDate));
    }

    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<List<ScheduleEntryDTO>> getScheduleForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(scheduleService.getScheduleForDate(date));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<List<ScheduleEntryDTO>> getScheduleForDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(scheduleService.getScheduleForDateRange(startDate, endDate));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<ScheduleEntryDTO> createScheduleEntry(@Valid @RequestBody ScheduleEntryDTO entryDTO) {
        return new ResponseEntity<>(scheduleService.createScheduleEntry(entryDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<ScheduleEntryDTO> updateScheduleEntry(
            @PathVariable String id,
            @Valid @RequestBody ScheduleEntryDTO entryDTO) {

        return ResponseEntity.ok(scheduleService.updateScheduleEntry(id, entryDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<Void> deleteScheduleEntry(@PathVariable String id) {
        scheduleService.deleteScheduleEntry(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/settings")
    public ResponseEntity<BusinessSettings> getBusinessSettings() {
        return ResponseEntity.ok(scheduleService.getBusinessSettings());
    }

    @PutMapping("/settings")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BusinessSettings> updateBusinessSettings(@RequestBody BusinessSettings settings) {
        return ResponseEntity.ok(scheduleService.updateBusinessSettings(settings));
    }
}