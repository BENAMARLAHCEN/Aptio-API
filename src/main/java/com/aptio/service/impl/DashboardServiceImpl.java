package com.aptio.service.impl;

import com.aptio.dto.AppointmentDTO;
import com.aptio.dto.DashboardStatsDTO;
import com.aptio.model.Appointment;
import com.aptio.model.Customer;
import com.aptio.model.Staff;
import com.aptio.repository.AppointmentRepository;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.StaffRepository;
import com.aptio.service.DashboardService;
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
public class DashboardServiceImpl implements DashboardService {

    private final AppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final ModelMapper modelMapper;
    
    public DashboardStatsDTO getDashboardStats() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        List<Appointment> recentAppointments = appointmentRepository.findByDateBetween(thirtyDaysAgo, LocalDate.now());
        int totalAppointments = recentAppointments.size();

        LocalDateTime thirtyDaysAgoDateTime = LocalDateTime.now().minusDays(30);
        List<Customer> newCustomers = customerRepository.findByRegistrationDateAfter(thirtyDaysAgoDateTime);
        int newCustomerCount = newCustomers.size();

        double utilizationRate = calculateUtilizationRate();

        double averageFeedback = 4.8;

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

 
    private double calculateUtilizationRate() {
        List<Staff> activeStaff = staffRepository.findByIsActive(true);
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDate today = LocalDate.now();
        long totalWorkingMinutes = calculateTotalPotentialWorkingMinutes(activeStaff, thirtyDaysAgo, today);
        List<Appointment> appointments = appointmentRepository.findByDateBetween(thirtyDaysAgo, today);
        long scheduledMinutes = appointments.stream()
                .filter(a -> a.getStatus() != Appointment.AppointmentStatus.CANCELLED)
                .mapToLong(a -> a.getService().getDuration())
                .sum();
        return totalWorkingMinutes > 0 ?
            new BigDecimal(scheduledMinutes * 100.0 / totalWorkingMinutes)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue() : 0;
    }

   
    private long calculateTotalPotentialWorkingMinutes(List<Staff> staff, LocalDate startDate, LocalDate endDate) {
        final long[] totalMinutes = {0};
        for (Staff member : staff) {
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                int dayOfWeek = currentDate.getDayOfWeek().getValue() % 7;
                member.getWorkHours().stream()
                        .filter(wh -> wh.getDayOfWeek() == dayOfWeek && wh.isWorking())
                        .findFirst()
                        .ifPresent(workHours -> {
                            long minutesInDay = ChronoUnit.MINUTES.between(
                                    workHours.getStartTime(),
                                    workHours.getEndTime());
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
   
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = modelMapper.map(appointment, AppointmentDTO.class);
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