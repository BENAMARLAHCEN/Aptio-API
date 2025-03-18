package com.aptio.service.impl;

import com.aptio.dto.ScheduleEntryDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
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

    public List<ScheduleEntryDTO> getStaffSchedule(String staffId, LocalDate startDate, LocalDate endDate) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff", "id", staffId);
        }

        return scheduleEntryRepository.findStaffSchedule(startDate, endDate, staffId).stream()
                .map(entry -> modelMapper.map(entry, ScheduleEntryDTO.class))
                .collect(Collectors.toList());
    }

    public List<ScheduleEntryDTO> getScheduleForDate(LocalDate date) {
        return scheduleEntryRepository.findByDate(date).stream()
                .map(entry -> modelMapper.map(entry, ScheduleEntryDTO.class))
                .collect(Collectors.toList());
    }

    public List<ScheduleEntryDTO> getScheduleForDateRange(LocalDate startDate, LocalDate endDate) {
        return scheduleEntryRepository.findByDateBetween(startDate, endDate).stream()
                .map(entry -> modelMapper.map(entry, ScheduleEntryDTO.class))
                .collect(Collectors.toList());
    }

    @Transactional
    public ScheduleEntryDTO createScheduleEntry(ScheduleEntryDTO entryDTO) {
        // Validate staff exists
        Staff staff = staffRepository.findById(entryDTO.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", entryDTO.getStaffId()));

        // Check for overlapping entries
        LocalDate date = entryDTO.getDate();
        LocalTime startTime = entryDTO.getStartTime();
        LocalTime endTime = entryDTO.getEndTime();

        List<ScheduleEntry> overlappingEntries = scheduleEntryRepository.findOverlappingEntriesForStaff(
                date, startTime, endTime, staff.getId());

        if (!overlappingEntries.isEmpty()) {
            throw new ValidationException("There are overlapping schedule entries for this time period");
        }

        // Create schedule entry
        ScheduleEntry entry = modelMapper.map(entryDTO, ScheduleEntry.class);
        entry.setStaff(staff);

        // Set resource if provided
        if (entryDTO.getResourceId() != null && !entryDTO.getResourceId().isEmpty()) {
            // Resource validation would go here
        }

        // Set default values if not provided
        if (entry.getType() == null) {
            entry.setType(ScheduleEntry.EntryType.OTHER);
        }

        if (entry.getStatus() == null) {
            entry.setStatus(ScheduleEntry.EntryStatus.SCHEDULED);
        }

        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        ScheduleEntry savedEntry = scheduleEntryRepository.save(entry);
        return modelMapper.map(savedEntry, ScheduleEntryDTO.class);
    }

    @Transactional
    public ScheduleEntryDTO updateScheduleEntry(String id, ScheduleEntryDTO entryDTO) {
        ScheduleEntry existingEntry = scheduleEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule entry", "id", id));

        // Check for overlapping entries if time changed
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

        // Update entry
        existingEntry.setTitle(entryDTO.getTitle());
        existingEntry.setDate(entryDTO.getDate());
        existingEntry.setStartTime(entryDTO.getStartTime());
        existingEntry.setEndTime(entryDTO.getEndTime());
        existingEntry.setNotes(entryDTO.getNotes());
        existingEntry.setColor(entryDTO.getColor());

        if (entryDTO.getType() != null) {
            existingEntry.setType(ScheduleEntry.EntryType.valueOf(entryDTO.getType()));
        }

        if (entryDTO.getStatus() != null) {
            existingEntry.setStatus(ScheduleEntry.EntryStatus.valueOf(entryDTO.getStatus()));
        }

        ScheduleEntry updatedEntry = scheduleEntryRepository.save(existingEntry);
        return modelMapper.map(updatedEntry, ScheduleEntryDTO.class);
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
            return; // No staff assigned, no schedule entry needed
        }

        // Calculate end time
        LocalTime endTime = appointment.getTime().plusMinutes(appointment.getService().getDuration());

        // Create schedule entry
        ScheduleEntry entry = ScheduleEntry.builder()
                .staff(appointment.getStaff())
                .appointment(appointment)
                .title(appointment.getService().getName() + " - " +
                        appointment.getCustomer().getFirstName() + " " +
                        appointment.getCustomer().getLastName())
                .date(appointment.getDate())
                .startTime(appointment.getTime())
                .endTime(endTime)
                .notes(appointment.getNotes())
                .type(ScheduleEntry.EntryType.APPOINTMENT)
                .status(mapAppointmentStatusToEntryStatus(appointment.getStatus()))
                .createdAt(appointment.getCreatedAt())
                .updatedAt(appointment.getUpdatedAt())
                .build();

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

        // Update fields
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

    /**
     * Maps appointment status to schedule entry status
     */
    private ScheduleEntry.EntryStatus mapAppointmentStatusToEntryStatus(Appointment.AppointmentStatus status) {
        switch (status) {
            case PENDING:
            case CONFIRMED:
                return ScheduleEntry.EntryStatus.SCHEDULED;
            case COMPLETED:
                return ScheduleEntry.EntryStatus.COMPLETED;
            case CANCELLED:
                return ScheduleEntry.EntryStatus.CANCELLED;
            default:
                return ScheduleEntry.EntryStatus.SCHEDULED;
        }
    }
}