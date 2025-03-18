package com.aptio.controller;

import com.aptio.dto.AppointmentDTO;
import com.aptio.dto.UserAppointmentRequestDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.model.Appointment;
import com.aptio.model.Customer;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.UserRepository;
import com.aptio.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * REST controller for managing user appointments.
 * These endpoints are specifically for customers to manage their own appointments.
 */
@RestController
@RequestMapping("/user/appointments")
@RequiredArgsConstructor
public class UserAppointmentController {

    private final AppointmentService appointmentService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /**
     * Get all appointments for the current authenticated user.
     *
     * @param status Optional status filter
     * @return List of appointments for the user
     */
    @GetMapping
    public ResponseEntity<List<AppointmentDTO>> getCurrentUserAppointments(
            @RequestParam(required = false) String status) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Get customer record for this user
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

    /**
     * Get a specific appointment for the current user.
     *
     * @param id Appointment ID
     * @return Appointment details if the appointment belongs to the user
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentDTO> getUserAppointmentById(@PathVariable String id) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Get customer record for this user
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));



        // Get the appointment
        AppointmentDTO appointment = appointmentService.getAppointmentById(id);

        // Verify this appointment belongs to the current user
        if (!appointment.getCustomerId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to view this appointment");
        }

        return ResponseEntity.ok(appointment);
    }

    /**
     * Create a new appointment for the current user.
     *
     * @param appointmentRequest Appointment details
     * @return Created appointment
     */
    @PostMapping
    public ResponseEntity<AppointmentDTO> createUserAppointment(
            @Valid @RequestBody UserAppointmentRequestDTO appointmentRequest) {

        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Get customer record for this user
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));

        // Create appointment DTO with customer ID
        AppointmentDTO appointmentDTO = AppointmentDTO.builder()
                .customerId(customer.getId())
                .serviceId(appointmentRequest.getServiceId())
                .staffId(appointmentRequest.getStaffId())
                .date(appointmentRequest.getDate())
                .time(appointmentRequest.getTime())
                .notes(appointmentRequest.getNotes())
                .build();

        // Create the appointment
        return new ResponseEntity<>(appointmentService.createAppointment(appointmentDTO), HttpStatus.CREATED);
    }

    /**
     * Cancel an appointment for the current user.
     * Users can only cancel their own upcoming appointments.
     *
     * @param id Appointment ID
     * @return Updated appointment
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentDTO> cancelUserAppointment(@PathVariable String id) {
        // Get current authenticated user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userEmail = authentication.getName();

        // Get customer record for this user
        Customer customer = customerRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", userEmail));

        // Get the appointment
        AppointmentDTO appointment = appointmentService.getAppointmentById(id);

        // Verify this appointment belongs to the current user
        if (!appointment.getCustomerId().equals(customer.getId())) {
            throw new AccessDeniedException("You do not have permission to modify this appointment");
        }

        // Verify the appointment can be cancelled
        if (appointment.getStatus().equalsIgnoreCase("COMPLETED")) {
            throw new ValidationException("Cannot cancel a completed appointment");
        }

        // Verify the appointment is not already cancelled
        if (appointment.getStatus().equalsIgnoreCase("CANCELLED")) {
            throw new ValidationException("Appointment is already cancelled");
        }

        // Verify the appointment is in the future
        LocalDateTime appointmentDateTime = LocalDateTime.of(
                appointment.getDate(),
                appointment.getTime()
        );

        if (appointmentDateTime.isBefore(LocalDateTime.now())) {
            throw new ValidationException("Cannot cancel a past appointment");
        }

        // Update the status to cancelled
        return ResponseEntity.ok(appointmentService.updateAppointmentStatus(id, "CANCELLED"));
    }

    /**
     * Get available time slots for a specific service and date.
     *
     * @param date Service date
     * @param serviceId Service ID
     * @param staffId Optional staff ID
     * @return List of available time slots
     */
    @GetMapping("/available-slots")
    public ResponseEntity<List<String>> getAvailableTimeSlots(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String serviceId,
            @RequestParam(required = false) String staffId) {

        return ResponseEntity.ok(appointmentService.getAvailableTimeSlots(date, serviceId, staffId));
    }
}