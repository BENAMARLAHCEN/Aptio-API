package com.aptio.repository;

import com.aptio.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByCustomerId(String customerId);

    List<Appointment> findByStaffId(String staffId);

    List<Appointment> findByStatus(Appointment.AppointmentStatus status);

    List<Appointment> findByDate(LocalDate date);

    List<Appointment> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Query("SELECT a FROM Appointment a WHERE a.date = :date AND a.staff.id = :staffId")
    List<Appointment> findByDateAndStaffId(LocalDate date, String staffId);

    @Query("SELECT a FROM Appointment a WHERE a.date = :date " +
            "AND ((a.time >= :startTime AND a.time < :endTime) OR " +
            "(FUNCTION('ADDTIME', a.time, FUNCTION('SEC_TO_TIME', a.service.duration * 60)) > :startTime " +
            "AND a.time < :endTime))")
    List<Appointment> findOverlappingAppointments(LocalDate date, LocalTime startTime, LocalTime endTime);

    @Query("SELECT a FROM Appointment a WHERE a.date = :date " +
            "AND a.staff.id = :staffId " +
            "AND ((a.time >= :startTime AND a.time < :endTime) OR " +
            "(FUNCTION('ADDTIME', a.time, FUNCTION('SEC_TO_TIME', a.service.duration * 60)) > :startTime " +
            "AND a.time < :endTime))")
    List<Appointment> findOverlappingAppointmentsForStaff(LocalDate date, LocalTime startTime, LocalTime endTime, String staffId);

    List<Appointment> findByCustomerIdAndStatus(String customerId, Appointment.AppointmentStatus status);
}