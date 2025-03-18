package com.aptio.service;

import com.aptio.dto.ScheduleEntryDTO;
import com.aptio.model.Appointment;
import com.aptio.model.BusinessSettings;
import com.aptio.model.ScheduleEntry;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleService {

    List<ScheduleEntryDTO> getStaffSchedule(String staffId, LocalDate startDate, LocalDate endDate);

    List<ScheduleEntryDTO> getScheduleForDate(LocalDate date);

    List<ScheduleEntryDTO> getScheduleForDateRange(LocalDate startDate, LocalDate endDate);

    ScheduleEntryDTO createScheduleEntry(ScheduleEntryDTO entryDTO);

    ScheduleEntryDTO updateScheduleEntry(String id, ScheduleEntryDTO entryDTO);

    void deleteScheduleEntry(String id);

    void createAppointmentScheduleEntry(Appointment appointment);

    void deleteAppointmentScheduleEntries(String appointmentId);

    void updateAppointmentScheduleEntryStatus(String appointmentId, ScheduleEntry.EntryStatus status);

    BusinessSettings getBusinessSettings();

    BusinessSettings updateBusinessSettings(BusinessSettings settings);
}