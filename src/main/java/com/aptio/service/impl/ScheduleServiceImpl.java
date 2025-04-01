package com.aptio.service.impl;

import com.aptio.dto.ResponseScheduleEntryDTO;
import com.aptio.dto.ScheduleEntryDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.mapper.ScheduleMapper;
import com.aptio.model.Appointment;
import com.aptio.model.BusinessSettings;
import com.aptio.model.ScheduleEntry;
import com.aptio.model.Staff;
import com.aptio.repository.BusinessSettingsRepository;
import com.aptio.repository.ScheduleEntryRepository;
import com.aptio.repository.StaffRepository;
import com.aptio.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleEntryRepository scheduleEntryRepository;
    private final StaffRepository staffRepository;
    private final BusinessSettingsRepository settingsRepository;
    private final ModelMapper modelMapper;
    private final ScheduleMapper scheduleMapper;

    public List<ResponseScheduleEntryDTO> getStaffSchedule(String staffId, LocalDate startDate, LocalDate endDate) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff", "id", staffId);
        }
        List<ScheduleEntry> staffSchedule = scheduleEntryRepository.findStaffSchedule(startDate, endDate, staffId);
        List<ScheduleEntryDTO> dtos = staffSchedule.stream()
                .map(scheduleMapper::toDTO)
                .collect(Collectors.toList());
        List<ResponseScheduleEntryDTO> response = dtos.stream()
                .map(dto -> modelMapper.map(dto, ResponseScheduleEntryDTO.class))
                .collect(Collectors.toList());

        return response;
    }

    public List<ScheduleEntryDTO> getScheduleForDate(LocalDate date) {
        return scheduleEntryRepository.findByDate(date).stream()
                .map(scheduleMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<ScheduleEntryDTO> getScheduleForDateRange(LocalDate startDate, LocalDate endDate) {
        return scheduleEntryRepository.findByDateBetween(startDate, endDate).stream()
                .map(scheduleMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleEntryDTO createScheduleEntry(ScheduleEntryDTO entryDTO) {
        Staff staff = staffRepository.findById(entryDTO.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", entryDTO.getStaffId()));
        LocalDate date = entryDTO.getDate();
        LocalTime startTime = entryDTO.getStartTime();
        LocalTime endTime = entryDTO.getEndTime();

        List<ScheduleEntry> overlappingEntries = scheduleEntryRepository.findOverlappingEntriesForStaff(
                date, startTime, endTime, staff.getId());

        if (!overlappingEntries.isEmpty()) {
            throw new ValidationException("There are overlapping schedule entries for this time period");
        }
        ScheduleEntry entry = scheduleMapper.toEntity(entryDTO);
        entry.setStaff(staff);

        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        ScheduleEntry savedEntry = scheduleEntryRepository.save(entry);
        return scheduleMapper.toDTO(savedEntry);
    }

    @Transactional
    public ScheduleEntryDTO updateScheduleEntry(String id, ScheduleEntryDTO entryDTO) {
        ScheduleEntry existingEntry = scheduleEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule entry", "id", id));
        boolean timeChanged = !existingEntry.getDate().equals(entryDTO.getDate()) ||
                !existingEntry.getStartTime().equals(entryDTO.getStartTime()) ||
                !existingEntry.getEndTime().equals(entryDTO.getEndTime());

        if (timeChanged) {
            List<ScheduleEntry> overlappingEntries = scheduleEntryRepository.findOverlappingEntriesForStaff(
                            entryDTO.getDate(), entryDTO.getStartTime(), entryDTO.getEndTime(),
                            existingEntry.getStaff().getId()).stream()
                    .filter(entry -> !entry.getId().equals(id))
                    .collect(Collectors.toList());

            if (!overlappingEntries.isEmpty()) {
                throw new ValidationException("There are overlapping schedule entries for this time period");
            }
        }
        scheduleMapper.updateEntityFromDTO(entryDTO, existingEntry);
        existingEntry.setUpdatedAt(LocalDateTime.now());

        ScheduleEntry updatedEntry = scheduleEntryRepository.save(existingEntry);
        return scheduleMapper.toDTO(updatedEntry);
    }

    @Transactional
    public void deleteScheduleEntry(String id) {
        if (!scheduleEntryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Schedule entry", "id", id);
        }
        scheduleEntryRepository.deleteById(id);
    }

    /**
     * Creates a schedule entry for an appointment
     */
    @Transactional
    public void createAppointmentScheduleEntry(Appointment appointment) {
        if (appointment.getStaff() == null) {
            return;
        }
        ScheduleEntry entry = scheduleMapper.createEntryFromAppointment(appointment, appointment.getStaff());
        scheduleEntryRepository.save(entry);
    }

    /**
     * Deletes all schedule entries for an appointment
     */
    @Transactional
    public void deleteAppointmentScheduleEntries(String appointmentId) {
        List<ScheduleEntry> entries = scheduleEntryRepository.findByAppointmentId(appointmentId);
        scheduleEntryRepository.deleteAll(entries);
    }

    /**
     * Updates the status of schedule entries for an appointment
     */
    @Transactional
    public void updateAppointmentScheduleEntryStatus(String appointmentId, ScheduleEntry.EntryStatus status) {
        List<ScheduleEntry> entries = scheduleEntryRepository.findByAppointmentId(appointmentId);

        for (ScheduleEntry entry : entries) {
            entry.setStatus(status);
        }

        scheduleEntryRepository.saveAll(entries);
    }

    /**
     * Gets the business settings
     */
    public BusinessSettings getBusinessSettings() {
        return settingsRepository.findFirstByOrderById();
    }

    /**
     * Updates the business settings
     */
    @Transactional
    public BusinessSettings updateBusinessSettings(BusinessSettings settings) {
        BusinessSettings existingSettings = settingsRepository.findFirstByOrderById();

        if (existingSettings == null) {
            return settingsRepository.save(settings);
        }
        existingSettings.setBusinessName(settings.getBusinessName());
        existingSettings.setBusinessHoursStart(settings.getBusinessHoursStart());
        existingSettings.setBusinessHoursEnd(settings.getBusinessHoursEnd());
        existingSettings.setDaysOpen(settings.getDaysOpen());
        existingSettings.setDefaultAppointmentDuration(settings.getDefaultAppointmentDuration());
        existingSettings.setTimeSlotInterval(settings.getTimeSlotInterval());
        existingSettings.setAllowOverlappingAppointments(settings.isAllowOverlappingAppointments());
        existingSettings.setBufferTimeBetweenAppointments(settings.getBufferTimeBetweenAppointments());
        existingSettings.setAddress(settings.getAddress());
        existingSettings.setPhone(settings.getPhone());
        existingSettings.setEmail(settings.getEmail());
        existingSettings.setWebsite(settings.getWebsite());

        return settingsRepository.save(existingSettings);
    }
}