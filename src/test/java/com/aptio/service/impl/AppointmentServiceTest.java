package com.aptio.service.impl;

import com.aptio.dto.AppointmentDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.model.*;
import com.aptio.repository.AppointmentRepository;
import com.aptio.repository.CustomerRepository;
import com.aptio.repository.ServiceRepository;
import com.aptio.repository.StaffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private ScheduleServiceImpl scheduleService;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Customer customer;
    private Service service;
    private Staff staff;
    private User staffUser;
    private Appointment appointment;
    private AppointmentDTO appointmentDTO;
    private BusinessSettings businessSettings;

    @BeforeEach
    void setUp() {

        customer = Customer.builder()
                .id("1")
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("1234567890")
                .active(true)
                .build();

        ServiceCategory category = ServiceCategory.builder()
                .id("1")
                .name("Haircut")
                .build();

        service = Service.builder()
                .id("1")
                .name("Men's Haircut")
                .description("Standard men's haircut")
                .duration(30)
                .price(new BigDecimal("25.00"))
                .category(category)
                .active(true)
                .build();

        staffUser = User.builder()
                .id("1")
                .firstName("Jane")
                .lastName("Smith")
                .email("jane.smith@example.com")
                .build();

        staff = Staff.builder()
                .id("1")
                .user(staffUser)
                .position("Stylist")
                .specialties(new HashSet<>())
                .isActive(true)
                .workHours(createDefaultWorkHours())
                .build();

        appointment = Appointment.builder()
                .id("1")
                .customer(customer)
                .service(service)
                .staff(staff)
                .date(LocalDate.now().plusDays(1))
                .time(LocalTime.of(10, 0))
                .status(Appointment.AppointmentStatus.PENDING)
                .price(service.getPrice())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        appointmentDTO = new AppointmentDTO();
        appointmentDTO.setId("1");
        appointmentDTO.setCustomerId(customer.getId());
        appointmentDTO.setServiceId(service.getId());
        appointmentDTO.setStaffId(staff.getId());
        appointmentDTO.setDate(LocalDate.now().plusDays(1));
        appointmentDTO.setTime(LocalTime.of(10, 0));
        appointmentDTO.setStatus("PENDING");
        appointmentDTO.setPrice(service.getPrice());


        businessSettings = BusinessSettings.builder()
                .id(1L)
                .businessName("Test Salon")
                .businessHoursStart(LocalTime.of(9, 0))
                .businessHoursEnd(LocalTime.of(18, 0))
                .daysOpen("0111110")
                .defaultAppointmentDuration(30)
                .timeSlotInterval(15)
                .allowOverlappingAppointments(false)
                .bufferTimeBetweenAppointments(5)
                .build();
    }

    private List<WorkHours> createDefaultWorkHours() {
        List<WorkHours> workHours = new ArrayList<>();


        for (int i = 0; i < 7; i++) {
            boolean isWorkDay = i > 0 && i < 6;

            WorkHours dayHours = WorkHours.builder()
                    .id((long) i)
                    .dayOfWeek(i)
                    .isWorking(isWorkDay)
                    .startTime(isWorkDay ? LocalTime.of(9, 0) : null)
                    .endTime(isWorkDay ? LocalTime.of(17, 0) : null)
                    .breaks(new ArrayList<>())
                    .build();


            if (isWorkDay) {
                TimeSlot lunchBreak = TimeSlot.builder()
                        .id(1L)
                        .startTime(LocalTime.of(12, 0))
                        .endTime(LocalTime.of(13, 0))
                        .note("Lunch break")
                        .workHours(dayHours)
                        .build();

                dayHours.getBreaks().add(lunchBreak);
            }

            workHours.add(dayHours);
        }

        return workHours;
    }

    @Test
    void getAppointmentById_WhenAppointmentDoesNotExist_ShouldThrowException() {

        when(appointmentRepository.findById("999")).thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> appointmentService.getAppointmentById("999"));
        verify(appointmentRepository).findById("999");
    }

    @Test
    void createAppointment_WithNonExistentCustomer_ShouldThrowException() {

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(appointmentDTO));
        verify(customerRepository).findById(customer.getId());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void createAppointment_WithNonExistentService_ShouldThrowException() {

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(appointmentDTO));
        verify(serviceRepository).findById(service.getId());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void createAppointment_WithNonExistentStaff_ShouldThrowException() {

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> appointmentService.createAppointment(appointmentDTO));
        verify(staffRepository).findById(staff.getId());
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void createAppointment_WithUnavailableTimeSlot_ShouldThrowException() {

        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(scheduleService.getBusinessSettings()).thenReturn(businessSettings);


        List<Appointment> overlappingAppointments = new ArrayList<>();
        overlappingAppointments.add(Appointment.builder()
                .id("2")
                .customer(customer)
                .service(service)
                .staff(staff)
                .date(appointmentDTO.getDate())
                .time(appointmentDTO.getTime())
                .status(Appointment.AppointmentStatus.CONFIRMED)
                .build());

        when(appointmentRepository.findByDateAndStaffId(any(), any())).thenReturn(overlappingAppointments);


        assertThrows(ValidationException.class, () -> appointmentService.createAppointment(appointmentDTO));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }


    @Test
    void updateAppointmentStatus_WithInvalidStatus_ShouldThrowException() {

        when(appointmentRepository.findById("1")).thenReturn(Optional.of(appointment));


        assertThrows(ValidationException.class, () -> appointmentService.updateAppointmentStatus("1", "INVALID_STATUS"));
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    void deleteAppointment_WhenAppointmentExists_ShouldDeleteAppointment() {

        when(appointmentRepository.findById("1")).thenReturn(Optional.of(appointment));


        appointmentService.deleteAppointment("1");


        verify(scheduleService).deleteAppointmentScheduleEntries("1");
        verify(appointmentRepository).deleteById("1");
    }

    @Test
    void deleteAppointment_WhenAppointmentDoesNotExist_ShouldThrowException() {

        when(appointmentRepository.findById("999")).thenReturn(Optional.empty());


        assertThrows(ResourceNotFoundException.class, () -> appointmentService.deleteAppointment("999"));
        verify(appointmentRepository, never()).deleteById(anyString());
    }

    @Test
    void getAvailableTimeSlots_OnBusinessDay_ShouldReturnAvailableSlots() {

        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(scheduleService.getBusinessSettings()).thenReturn(businessSettings);
        when(staffRepository.findById(staff.getId())).thenReturn(Optional.of(staff));
        when(appointmentRepository.findByDateAndStaffId(any(), any())).thenReturn(new ArrayList<>());

        LocalDate businessDay = LocalDate.now().plusDays(1);
        while (businessDay.getDayOfWeek().getValue() % 7 == 0) {
            businessDay = businessDay.plusDays(1);
        }


        List<String> result = appointmentService.getAvailableTimeSlots(businessDay, service.getId(), staff.getId());


        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void getAvailableTimeSlots_OnClosedDay_ShouldReturnEmptyList() {

        when(serviceRepository.findById(service.getId())).thenReturn(Optional.of(service));
        when(scheduleService.getBusinessSettings()).thenReturn(businessSettings);


        LocalDate sunday = LocalDate.now();
        while (sunday.getDayOfWeek().getValue() % 7 != 0) {
            sunday = sunday.plusDays(1);
        }


        List<String> result = appointmentService.getAvailableTimeSlots(sunday, service.getId(), staff.getId());


        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getAppointmentsByCustomerId_WithNonExistentCustomer_ShouldThrowException() {

        when(customerRepository.existsById("999")).thenReturn(false);


        assertThrows(ResourceNotFoundException.class, () -> appointmentService.getAppointmentsByCustomerId("999"));
    }
}