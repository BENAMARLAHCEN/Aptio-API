package com.aptio.service;

import com.aptio.dto.AppointmentDTO;
import com.aptio.dto.DashboardStatsDTO;
import com.aptio.model.Appointment;
import com.aptio.model.Customer;
import com.aptio.model.Staff;
import com.aptio.repository.AppointmentRepository;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ModelMapper modelMapper;

    /**
     * Get dashboard statistics
     * @return DashboardStatsDTO with statistics
     */
    public DashboardStatsDTO getDashboardStats() {
        // Count appointments in the last 30 days
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Appointment> recentAppointments = appointmentRepository.findByDateBetween(thirtyDaysAgo, LocalDate.now());
        int totalAppointments = recentAppointments.size();

        // Count new customers in the last 30 days
        LocalDateTime thirtyDaysAgoDateTime = LocalDateTime.now().minusDays(30);
        List<Customer> newCustomers = customerRepository.findByRegistrationDateAfter(thirtyDaysAgoDateTime);
        int newCustomerCount = newCustomers.size();

        // Calculate utilization rate (percentage of working hours filled with appointments)
        double utilizationRate = calculateUtilizationRate();

        // Placeholder for average feedback (would need a feedback system)
        double averageFeedback = 4.8; // Placeholder value

        // Get today's appointments for the dashboard
        LocalDate today = LocalDate.now();
        List<Appointment> todayAppointments = appointmentRepository.findByDate(today);
        List<AppointmentDTO> recentAppointmentDTOs = todayAppointments.stream()
                .limit(5)
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .totalAppointments(totalAppointments)
                .newCustomers(newCustomerCount)
                .utilizationRate(utilizationRate)
                .averageFeedback(averageFeedback)
                .recentAppointments(recentAppointmentDTOs)
                .build();
    }

    /**
     * Calculate utilization rate based on appointments vs available time slots
     * @return Utilization rate as a percentage
     */
    private double calculateUtilizationRate() {
        // Calculate total available working minutes across all staff in the past 30 days
        List<Staff> activeStaff = staffRepository.findByIsActive(true);
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate today = LocalDate.now();

        // Calculate total potential working minutes
        long totalWorkingMinutes = calculateTotalPotentialWorkingMinutes(activeStaff, thirtyDaysAgo, today);

        // Calculate minutes actually scheduled with appointments
        List<Appointment> appointments = appointmentRepository.findByDateBetween(thirtyDaysAgo, today);
        long scheduledMinutes = appointments.stream()
                .filter(a -> a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .mapToLong(a -> a.getService().getDuration())
                .sum();

        // Calculate utilization rate
        return totalWorkingMinutes > 0 ?
            new BigDecimal(scheduledMinutes * 100.0 / totalWorkingMinutes)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue() : 0;
    }

    /**
     * Calculate total potential working minutes for all staff in a date range
     */
    private long calculateTotalPotentialWorkingMinutes(List<Staff> staff, LocalDate startDate, LocalDate endDate) {
        final long[] totalMinutes = {0};

        // For each staff member
        for (Staff member : staff) {
            // For each day in the range
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                int dayOfWeek = currentDate.getDayOfWeek().getValue() % 7; // 0 = Sunday, 1 = Monday, etc.

                // Check if staff works on this day
                member.getWorkHours().stream()
                        .filter(wh -> wh.getDayOfWeek() == dayOfWeek && wh.isWorking())
                        .findFirst()
                        .ifPresent(workHours -> {
                            // Calculate minutes for this day
                            long minutesInDay = ChronoUnit.MINUTES.between(
                                    workHours.getStartTime(),
                                    workHours.getEndTime());

                            // Subtract break times
                            long breakMinutes = workHours.getBreaks().stream()
                                    .mapToLong(breakTime ->
                                            ChronoUnit.MINUTES.between(
                                                    breakTime.getStartTime(),
                                                    breakTime.getEndTime()))
                                    .sum();

                            totalMinutes[0] += (minutesInDay - breakMinutes);
                        });

                currentDate = currentDate.plusDays(1);
            }
        }

        return totalMinutes[0];
    }
    /**
     * Convert Appointment to AppointmentDTO
     */
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = modelMapper.map(appointment, AppointmentDTO.class);

        // Set additional fields
        dto.setCustomerName(appointment.getCustomer().getFirstName() + " " + appointment.getCustomer().getLastName());
        dto.setServiceName(appointment.getService().getName());
        dto.setDuration(appointment.getService().getDuration());

        if (appointment.getStaff() != null) {
            dto.setStaffName(appointment.getStaff().getUser().getFirstName() + " " +
                    appointment.getStaff().getUser().getLastName());
        }

        return dto;
    }
}