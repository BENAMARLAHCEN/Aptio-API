package com.aptio.service;

import com.aptio.dto.AppointmentDTO;
import com.aptio.model.Appointment;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {

    List<AppointmentDTO> getAllAppointments();

    AppointmentDTO getAppointmentById(String id);

    List<AppointmentDTO> getAppointmentsByCustomerId(String customerId);

    List<AppointmentDTO> getAppointmentsByStaffId(String staffId);

    List<AppointmentDTO> getAppointmentsByDate(LocalDate date);

    List<AppointmentDTO> getAppointmentsByDateRange(LocalDate startDate, LocalDate endDate);

    List<AppointmentDTO> getAppointmentsByStatus(Appointment.AppointmentStatus status);

    AppointmentDTO createAppointment(AppointmentDTO appointmentDTO);

    AppointmentDTO updateAppointment(String id, AppointmentDTO appointmentDTO);

    AppointmentDTO updateAppointmentStatus(String id, String status);

    void deleteAppointment(String id);

    List<String> getAvailableTimeSlots(LocalDate date, String serviceId, String staffId);

    List<AppointmentDTO> getAppointmentsByCustomerIdAndStatus(String customerId, Appointment.AppointmentStatus status);
}