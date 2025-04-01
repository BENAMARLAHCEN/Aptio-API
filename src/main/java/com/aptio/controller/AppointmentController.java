package com.aptio.controller;

import com.aptio.dto.AppointmentDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.model.Appointment;
import com.aptio.model.Customer;
import com.aptio.model.User;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.UserRepository;
import com.aptio.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<List<AppointmentDTO>> getAllAppointments(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String staffId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String status) {

        if (customerId != null && !customerId.isEmpty()) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByCustomerId(customerId));
        } else if (staffId != null && !staffId.isEmpty()) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByStaffId(staffId));
        } else if (date != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByDate(date));
        } else if (startDate != null && endDate != null) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByDateRange(startDate, endDate));
        } else if (status != null && !status.isEmpty()) {
            return ResponseEntity.ok(appointmentService.getAppointmentsByStatus(
                    com.aptio.model.Appointment.AppointmentStatus.valueOf(status.toUpperCase())));
        } else {
            return ResponseEntity.ok(appointmentService.getAllAppointments());
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<AppointmentDTO> getAppointmentById(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<AppointmentDTO> createAppointment(@Valid @RequestBody AppointmentDTO appointmentDTO) {
        return new ResponseEntity<>(appointmentService.createAppointment(appointmentDTO), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<AppointmentDTO> updateAppointment(@PathVariable String id, @Valid @RequestBody AppointmentDTO appointmentDTO) {
        return ResponseEntity.ok(appointmentService.updateAppointment(id, appointmentDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<Void> deleteAppointment(@PathVariable String id) {
        appointmentService.deleteAppointment(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_STAFF')")
    public ResponseEntity<AppointmentDTO> updateAppointmentStatus(@PathVariable String id, @RequestBody Map<String, String> statusMap) {
        String status = statusMap.getOrDefault("status", "PENDING");
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, status));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String serviceId,
            @RequestParam(required = false) String staffId) {
        return ResponseEntity.ok(appointmentService.getAvailableTimeSlots(date, serviceId, staffId));
    }

    @GetMapping("/user/appointments")
    public ResponseEntity<List<AppointmentDTO>> getCurrentUserAppointments(
            @RequestParam(required = false) String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));

        List<AppointmentDTO> appointments;
        if (status != null && !status.isEmpty()) {
            appointments = appointmentService.getAppointmentsByCustomerIdAndStatus(
                    customer.getId(),
                    Appointment.AppointmentStatus.valueOf(status.toUpperCase())
            );
        } else {
            appointments = appointmentService.getAppointmentsByCustomerId(customer.getId());
        }

        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/user/appointments/{id}")
    public ResponseEntity<AppointmentDTO> getUserAppointmentById(@PathVariable String id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));
        AppointmentDTO appointment = appointmentService.getAppointmentById(id);
        if (!appointment.getCustomerId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to view this appointment");
        }

        return ResponseEntity.ok(appointment);
    }

    @PatchMapping("/user/appointments/{id}/status")
    public ResponseEntity<AppointmentDTO> updateUserAppointmentStatus(
            @PathVariable String id,
            @RequestBody Map<String, String> statusMap) {

        String status = statusMap.getOrDefault("status", "CANCELLED");
        if (!status.equalsIgnoreCase("CANCELLED")) {
            throw new ValidationException("Users can only cancel their appointments");
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));
        AppointmentDTO appointment = appointmentService.getAppointmentById(id);
        if (!appointment.getCustomerId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this appointment");
        }
        if (appointment.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new ValidationException("Cannot cancel a completed appointment");
        }
        if (appointment.getStatus().equalsIgnoreCase("CANCELLED")) {
            throw new ValidationException("Appointment is already cancelled");
        }
        LocalDateTime appointmentDateTime = LocalDateTime.of(
                appointment.getDate(),
                appointment.getTime()
        );

        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Cannot cancel a past appointment");
        }
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, status));
    }

    @PostMapping("/user/appointments")
    public ResponseEntity<AppointmentDTO> createUserAppointment(@Valid @RequestBody AppointmentDTO appointmentDTO) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));
        appointmentDTO.setCustomerId(customer.getId());
        return new ResponseEntity<>(appointmentService.createAppointment(appointmentDTO), HttpStatus.CREATED);
    }
}