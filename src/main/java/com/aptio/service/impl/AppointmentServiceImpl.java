package com.aptio.service.impl;

import com.aptio.dto.AppointmentDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.model.*;
import com.aptio.repository.AppointmentRepository;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.ServiceRepository;
import com.aptio.repository.StaffRepository;
import com.aptio.service.AppointmentService;
import com.aptio.util.TimeUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRepository serviceRepository;
    private final StaffRepository staffRepository;
    private final ScheduleServiceImpl scheduleService;
    private final ModelMapper modelMapper;

    public List<AppointmentDTO> getAllAppointments() {
        return appointmentRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public AppointmentDTO getAppointmentById(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));
        return convertToDTO(appointment);
    }

    public List<AppointmentDTO> getAppointmentsByCustomerId(String customerId) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "id", customerId);
        }
        return appointmentRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getAppointmentsByStaffId(String staffId) {
        if (!staffRepository.existsById(staffId)) {
            throw new ResourceNotFoundException("Staff", "id", staffId);
        }
        return appointmentRepository.findByStaffId(staffId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getAppointmentsByDate(LocalDate date) {
        return appointmentRepository.findByDate(date).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate) {
        return appointmentRepository.findByDateBetween(startDate, endDate).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<AppointmentDTO> getAppointmentsByStatus(Appointment.AppointmentStatus status) {
        return appointmentRepository.findByStatus(status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public AppointmentDTO createAppointment(AppointmentDTO appointmentDTO) {
        // Validate customer exists
        Customer customer = customerRepository.findById(appointmentDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", appointmentDTO.getCustomerId()));

        // Validate service exists
        com.aptio.model.Service service = serviceRepository.findById(appointmentDTO.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", appointmentDTO.getServiceId()));

        // Validate staff if provided
        Staff staff = null;
        if (appointmentDTO.getStaffId() != null && !appointmentDTO.getStaffId().isEmpty()) {
            staff = staffRepository.findById(appointmentDTO.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", appointmentDTO.getStaffId()));
        }

        // Check if time slot is available
        if (!isTimeSlotAvailable(appointmentDTO.getDate(), appointmentDTO.getTime(),
                service.getDuration(), appointmentDTO.getStaffId())) {
            throw new ValidationException("The selected time slot is not available");
        }

        // Create appointment
        Appointment appointment = new Appointment();
        appointment.setCustomer(customer);
        appointment.setService(service);
        appointment.setStaff(staff);
        appointment.setDate(appointmentDTO.getDate());
        appointment.setTime(appointmentDTO.getTime());
        appointment.setNotes(appointmentDTO.getNotes());
        appointment.setStatus(Appointment.AppointmentStatus.PENDING);
        appointment.setPrice(service.getPrice());
        appointment.setCreatedAt(TimeUtils.getCurrentTime());
        appointment.setUpdatedAt(TimeUtils.getCurrentTime());

        Appointment savedAppointment = appointmentRepository.save(appointment);

        // Create schedule entry for this appointment if staff is assigned
        if (staff != null) {
            scheduleService.createAppointmentScheduleEntry(savedAppointment);
        }

        return convertToDTO(savedAppointment);
    }

    @Transactional
    public AppointmentDTO updateAppointment(String id, AppointmentDTO appointmentDTO) {
        Appointment existingAppointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        // Validate customer exists
        Customer customer = customerRepository.findById(appointmentDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", appointmentDTO.getCustomerId()));

        // Validate service exists
        com.aptio.model.Service service = serviceRepository.findById(appointmentDTO.getServiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", appointmentDTO.getServiceId()));

        // Validate staff if provided
        Staff staff = null;
        if (appointmentDTO.getStaffId() != null && !appointmentDTO.getStaffId().isEmpty()) {
            staff = staffRepository.findById(appointmentDTO.getStaffId())
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", appointmentDTO.getStaffId()));
        }

        // Check if time slot is available (only if date or time changed)
        boolean timeChanged = !existingAppointment.getDate().equals(appointmentDTO.getDate()) ||
                !existingAppointment.getTime().equals(appointmentDTO.getTime());

        boolean staffChanged = (existingAppointment.getStaff() == null && staff != null) ||
                (existingAppointment.getStaff() != null && staff == null) ||
                (existingAppointment.getStaff() != null && staff != null &&
                        !existingAppointment.getStaff().getId().equals(staff.getId()));

        if (timeChanged || staffChanged) {
            if (!isTimeSlotAvailable(appointmentDTO.getDate(), appointmentDTO.getTime(),
                    service.getDuration(), appointmentDTO.getStaffId(), id)) {
                throw new ValidationException("The selected time slot is not available");
            }

            // Delete old schedule entry if exists
            if (existingAppointment.getStaff() != null) {
                scheduleService.deleteAppointmentScheduleEntries(id);
            }
        }

        // Update appointment
        existingAppointment.setCustomer(customer);
        existingAppointment.setService(service);
        existingAppointment.setStaff(staff);
        existingAppointment.setDate(appointmentDTO.getDate());
        existingAppointment.setTime(appointmentDTO.getTime());
        existingAppointment.setNotes(appointmentDTO.getNotes());
        existingAppointment.setPrice(service.getPrice());

        Appointment updatedAppointment = appointmentRepository.save(existingAppointment);

        // Create new schedule entry if needed
        if ((timeChanged || staffChanged) && staff != null) {
            scheduleService.createAppointmentScheduleEntry(updatedAppointment);
        }

        return convertToDTO(updatedAppointment);
    }

    @Transactional
    public AppointmentDTO updateAppointmentStatus(String id, String status) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        try {
            Appointment.AppointmentStatus newStatus = Appointment.AppointmentStatus.valueOf(status.toUpperCase());
            appointment.setStatus(newStatus);

            // If appointment is cancelled, also update any schedule entries
            if (newStatus == Appointment.AppointmentStatus.CANCELLED && appointment.getStaff() != null) {
                scheduleService.updateAppointmentScheduleEntryStatus(id, ScheduleEntry.EntryStatus.CANCELLED);
            } else if (newStatus == Appointment.AppointmentStatus.COMPLETED && appointment.getStaff() != null) {
                scheduleService.updateAppointmentScheduleEntryStatus(id, ScheduleEntry.EntryStatus.COMPLETED);
            }

            Appointment updatedAppointment = appointmentRepository.save(appointment);

            // Update customer stats if appointment is completed
            if (newStatus == Appointment.AppointmentStatus.COMPLETED) {
                updateCustomerStats(appointment.getCustomer().getId());
            }

            return convertToDTO(updatedAppointment);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid status: " + status);
        }
    }

    @Transactional
    public void deleteAppointment(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", "id", id));

        // Delete any schedule entries first
        if (appointment.getStaff() != null) {
            scheduleService.deleteAppointmentScheduleEntries(id);
        }

        appointmentRepository.deleteById(id);
    }



    public List<String> getAvailableTimeSlots(LocalDate date, String serviceId, String staffId) {
        com.aptio.model.Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "id", serviceId));

        int duration = service.getDuration();

        BusinessSettings settings = scheduleService.getBusinessSettings();
        boolean[] daysOpen = settings.getDaysOpenArray();
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 0 for Sunday, 1 for Monday, etc.

        if (!daysOpen[dayOfWeek]) {
            return new ArrayList<>();
        }

        LocalTime startTime = settings.getBusinessHoursStart();
        LocalTime endTime = settings.getBusinessHoursEnd();
        int interval = settings.getTimeSlotInterval();

        List<String> availableTimeSlots = new ArrayList<>();
        LocalTime currentTime = startTime;

        while (currentTime.plusMinutes(duration).isBefore(endTime) ||
                currentTime.plusMinutes(duration).equals(endTime)) {

            if (isTimeSlotAvailable(date, currentTime, duration, staffId)) {
                availableTimeSlots.add(currentTime.toString());
            }

            currentTime = currentTime.plusMinutes(duration);
        }

        return availableTimeSlots;
    }

    private boolean isTimeSlotAvailable(LocalDate date, LocalTime startTime, int duration, String staffId) {
        return isTimeSlotAvailable(date, startTime, duration, staffId, null);
    }

    private boolean isTimeSlotAvailable(LocalDate date, LocalTime startTime, int duration, String staffId, String excludeAppointmentId) {
        // Calculate end time
        LocalTime endTime = startTime.plusMinutes(duration);

        // Check business settings
        BusinessSettings settings = scheduleService.getBusinessSettings();
        boolean[] daysOpen = settings.getDaysOpenArray();
        int dayOfWeek = date.getDayOfWeek().getValue() % 7; // 0 for Sunday, 1 for Monday, etc.

        // If business is closed on this day, time slot is not available
        if (!daysOpen[dayOfWeek]) {
            return false;
        }

        // Check if time is within business hours
        LocalTime businessStart = settings.getBusinessHoursStart();
        LocalTime businessEnd = settings.getBusinessHoursEnd();

        if (startTime.isBefore(businessStart) || endTime.isAfter(businessEnd)) {
            return false;
        }

        // If staff is provided, check staff availability
        if (staffId != null && !staffId.isEmpty()) {
            Staff staff = staffRepository.findById(staffId)
                    .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", staffId));

            // Check if staff is working on this day
            WorkHours workHours = staff.getWorkHours().stream()
                    .filter(wh -> wh.getDayOfWeek() == dayOfWeek)
                    .findFirst()
                    .orElse(null);

            if (workHours == null || !workHours.isWorking()) {
                return false;
            }

            // Check if time is within staff work hours
            LocalTime workStart = workHours.getStartTime();
            LocalTime workEnd = workHours.getEndTime();

            if (startTime.isBefore(workStart) || endTime.isAfter(workEnd)) {
                return false;
            }

            // Check if time overlaps with staff breaks
            for (TimeSlot breakSlot : workHours.getBreaks()) {
                if (TimeUtils.doTimeRangesOverlap(startTime, endTime,
                        breakSlot.getStartTime(), breakSlot.getEndTime())) {
                    return false;
                }
            }

            // Check if staff already has appointments at this time
            List<Appointment> overlappingAppointments;
            if (excludeAppointmentId != null) {
                overlappingAppointments = appointmentRepository.findByDateAndStaffId(date, staffId).stream()
                        .filter(a -> !a.getId().equals(excludeAppointmentId) &&
                                a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                        .filter(a -> TimeUtils.doTimeRangesOverlap(startTime, endTime,
                                a.getTime(), a.getTime().plusMinutes(a.getService().getDuration())))
                        .collect(Collectors.toList());
            } else {
                overlappingAppointments = appointmentRepository.findByDateAndStaffId(date, staffId).stream()
                        .filter(a -> a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                        .filter(a -> TimeUtils.doTimeRangesOverlap(startTime, endTime,
                                a.getTime(), a.getTime().plusMinutes(a.getService().getDuration())))
                        .collect(Collectors.toList());
            }

            if (!overlappingAppointments.isEmpty()) {
                return false;
            }
        } else {
            // If no staff specified, check if there's any staff available at this time
            List<Staff> allStaff = staffRepository.findByIsActive(true);

            boolean anyStaffAvailable = allStaff.stream().anyMatch(staff -> {
                // Check if staff is working on this day
                WorkHours workHours = staff.getWorkHours().stream()
                        .filter(wh -> wh.getDayOfWeek() == dayOfWeek)
                        .findFirst()
                        .orElse(null);

                if (workHours == null || !workHours.isWorking()) {
                    return false;
                }

                // Check if time is within staff work hours
                LocalTime workStart = workHours.getStartTime();
                LocalTime workEnd = workHours.getEndTime();

                if (startTime.isBefore(workStart) || endTime.isAfter(workEnd)) {
                    return false;
                }

                // Check if time overlaps with staff breaks
                for (TimeSlot breakSlot : workHours.getBreaks()) {
                    if (TimeUtils.doTimeRangesOverlap(startTime, endTime,
                            breakSlot.getStartTime(), breakSlot.getEndTime())) {
                        return false;
                    }
                }

                // Check if staff already has appointments at this time
                List<Appointment> overlappingAppointments;
                if (excludeAppointmentId != null) {
                    overlappingAppointments = appointmentRepository.findByDateAndStaffId(date, staff.getId()).stream()
                            .filter(a -> !a.getId().equals(excludeAppointmentId) &&
                                    a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                            .filter(a -> TimeUtils.doTimeRangesOverlap(startTime, endTime,
                                    a.getTime(), a.getTime().plusMinutes(a.getService().getDuration())))
                            .collect(Collectors.toList());
                } else {
                    overlappingAppointments = appointmentRepository.findByDateAndStaffId(date, staff.getId()).stream()
                            .filter(a -> a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                            .filter(a -> TimeUtils.doTimeRangesOverlap(startTime, endTime,
                                    a.getTime(), a.getTime().plusMinutes(a.getService().getDuration())))
                            .collect(Collectors.toList());
                }

                return overlappingAppointments.isEmpty();
            });

            if (!anyStaffAvailable) {
                return false;
            }
        }

        return true;
    }

















    private void updateCustomerStats(String customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));

        // Get all completed appointments for this customer
        List<Appointment> completedAppointments = appointmentRepository.findByCustomerId(customerId).stream()
                .filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED)
                .collect(Collectors.toList());

        // Update customer stats
        customer.setTotalVisits(completedAppointments.size());

        BigDecimal totalSpent = completedAppointments.stream()
                .map(Appointment::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        customer.setTotalSpent(totalSpent);

        // Update last visit time
        if (!completedAppointments.isEmpty()) {
            customer.setLastVisit(completedAppointments.get(completedAppointments.size() - 1).getUpdatedAt());
        }

        customerRepository.save(customer);
    }

    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = modelMapper.map(appointment, AppointmentDTO.class);

        // Set additional fields
        dto.setCustomerName(appointment.getCustomer().getFirstName() + " " + appointment.getCustomer().getLastName());
        dto.setServiceName(appointment.getService().getName());
        dto.setDuration(appointment.getService().getDuration());
        dto.setCustomerId(appointment.getCustomer().getId());
        dto.setServiceId(appointment.getService().getId());

        if (appointment.getStaff() != null) {
            dto.setStaffName(appointment.getStaff().getUser().getFirstName() + " " +
                    appointment.getStaff().getUser().getLastName());
        }

        return dto;
    }

    public List<AppointmentDTO> getAppointmentsByCustomerIdAndStatus(String customerId, Appointment.AppointmentStatus status) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Customer", "id", customerId);
        }

        return appointmentRepository.findByCustomerIdAndStatus(customerId, status).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
}