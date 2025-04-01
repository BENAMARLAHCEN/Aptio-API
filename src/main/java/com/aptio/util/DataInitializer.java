package com.aptio.util;

import com.aptio.model.*;
import com.aptio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceCategoryRepository categoryRepository;
    private final ServiceRepository serviceRepository;
    private final CustomerRepository customerRepository;
    private final StaffRepository staffRepository;
    private final BusinessSettingsRepository settingsRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    @Transactional
    public void run(String... args) {

        initRoles();


        initAdminUser();


        initBusinessSettings();


        initServiceCategories();


        initSampleCustomers();


        initStaffMembers();


        initAppointments();
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(null, Role.RoleName.ROLE_USER));
            roleRepository.save(new Role(null, Role.RoleName.ROLE_STAFF));
            roleRepository.save(new Role(null, Role.RoleName.ROLE_ADMIN));
        }
    }

    private void initAdminUser() {
        if (!userRepository.existsByEmail("admin@aptio.com")) {

            User adminUser = User.builder()
                    .firstName("Admin")
                    .lastName("User")
                    .email("admin@aptio.com")
                    .password(passwordEncoder.encode("password"))
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();


            Set<Role> adminRoles = new HashSet<>();
            roleRepository.findByName(Role.RoleName.ROLE_ADMIN).ifPresent(adminRoles::add);
            adminUser.setRoles(adminRoles);

            userRepository.save(adminUser);
        }
    }

    private void initBusinessSettings() {
        if (settingsRepository.count() == 0) {
            BusinessSettings settings = BusinessSettings.builder()
                    .businessName("Aptio Appointment System")
                    .businessHoursStart(LocalTime.of(9, 0))
                    .businessHoursEnd(LocalTime.of(18, 0))
                    .daysOpen("0111110")
                    .defaultAppointmentDuration(30)
                    .timeSlotInterval(15)
                    .allowOverlappingAppointments(false)
                    .bufferTimeBetweenAppointments(5)
                    .address("123 Business St, City, State 12345")
                    .phone("555-123-4567")
                    .email("contact@aptio.com")
                    .website("https://aptio.com")
                    .build();

            settingsRepository.save(settings);
        }
    }

    private void initServiceCategories() {
        if (categoryRepository.count() == 0) {

            ServiceCategory hairCategory = ServiceCategory.builder()
                    .name("Hair")
                    .description("Hair cutting, styling, and coloring services")
                    .active(true)
                    .build();

            ServiceCategory nailsCategory = ServiceCategory.builder()
                    .name("Nails")
                    .description("Manicure, pedicure, and nail art services")
                    .active(true)
                    .build();

            ServiceCategory spaCategory = ServiceCategory.builder()
                    .name("Spa")
                    .description("Relaxation and wellness treatments")
                    .active(true)
                    .build();

            ServiceCategory consultationCategory = ServiceCategory.builder()
                    .name("Consultation")
                    .description("Style and beauty consultations")
                    .active(true)
                    .build();


            hairCategory = categoryRepository.save(hairCategory);
            nailsCategory = categoryRepository.save(nailsCategory);
            spaCategory = categoryRepository.save(spaCategory);
            consultationCategory = categoryRepository.save(consultationCategory);



            Service haircut = Service.builder()
                    .name("Haircut")
                    .description("Basic haircut service with styling")
                    .duration(30)
                    .price(new BigDecimal("35.00"))
                    .category(hairCategory)
                    .active(true)
                    .imageUrl("https://images.unsplash.com/photo-1596178060810-72660affe577?q=80&w=300")
                    .build();

            Service hairColoring = Service.builder()
                    .name("Hair Coloring")
                    .description("Full hair coloring service with premium products")
                    .duration(120)
                    .price(new BigDecimal("120.00"))
                    .category(hairCategory)
                    .active(true)
                    .imageUrl("https://images.unsplash.com/photo-1560966571-385ea47ff258?q=80&w=300")
                    .build();

            Service beardTrim = Service.builder()
                    .name("Beard Trim")
                    .description("Professional beard trimming and shaping")
                    .duration(20)
                    .price(new BigDecimal("15.00"))
                    .category(hairCategory)
                    .active(true)
                    .build();


            Service manicure = Service.builder()
                    .name("Manicure")
                    .description("Basic manicure service with polish")
                    .duration(45)
                    .price(new BigDecimal("25.00"))
                    .category(nailsCategory)
                    .active(true)
                    .imageUrl("https://images.unsplash.com/photo-1604654894610-df63bc536371?q=80&w=300")
                    .build();

            Service pedicure = Service.builder()
                    .name("Pedicure")
                    .description("Relaxing pedicure service with exfoliation")
                    .duration(60)
                    .price(new BigDecimal("35.00"))
                    .category(nailsCategory)
                    .active(true)
                    .build();

            Service gelNails = Service.builder()
                    .name("Gel Nails")
                    .description("Long-lasting gel nail application")
                    .duration(75)
                    .price(new BigDecimal("45.00"))
                    .category(nailsCategory)
                    .active(true)
                    .build();


            Service facial = Service.builder()
                    .name("Facial")
                    .description("Refreshing facial treatment for all skin types")
                    .duration(45)
                    .price(new BigDecimal("55.00"))
                    .category(spaCategory)
                    .active(true)
                    .imageUrl("https://images.unsplash.com/photo-1596178060671-7a80dc8059ea?q=80&w=300")
                    .build();

            Service massage = Service.builder()
                    .name("Massage")
                    .description("Full body relaxation massage")
                    .duration(60)
                    .price(new BigDecimal("70.00"))
                    .category(spaCategory)
                    .active(true)
                    .imageUrl("https://images.unsplash.com/photo-1600334129128-685c5582fd35?q=80&w=300")
                    .build();


            Service styleConsultation = Service.builder()
                    .name("Style Consultation")
                    .description("Personal style consultation with our experts")
                    .duration(30)
                    .price(new BigDecimal("40.00"))
                    .category(consultationCategory)
                    .active(false)
                    .build();


            serviceRepository.save(haircut);
            serviceRepository.save(hairColoring);
            serviceRepository.save(beardTrim);
            serviceRepository.save(manicure);
            serviceRepository.save(pedicure);
            serviceRepository.save(gelNails);
            serviceRepository.save(facial);
            serviceRepository.save(massage);
            serviceRepository.save(styleConsultation);
        }
    }

    private void initSampleCustomers() {
        if (customerRepository.count() == 0) {

            Address address1 = Address.builder()
                    .street("123 Main St")
                    .city("New York")
                    .state("NY")
                    .zipCode("10001")
                    .country("USA")
                    .build();

            Address address2 = Address.builder()
                    .street("456 Oak Ave")
                    .city("Los Angeles")
                    .state("CA")
                    .zipCode("90001")
                    .country("USA")
                    .build();


            Customer customer1 = Customer.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .email("john.doe@example.com")
                    .phone("555-123-4567")
                    .address(address1)
                    .birthDate(LocalDate.of(1985, 6, 15))
                    .gender("Male")
                    .profileImage("https://randomuser.me/api/portraits/men/1.jpg")
                    .active(true)
                    .totalVisits(5)
                    .totalSpent(new BigDecimal("350.00"))
                    .lastVisit(LocalDateTime.now().minusDays(5))
                    .registrationDate(LocalDateTime.now().minusMonths(3))
                    .build();

            Customer customer2 = Customer.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .phone("555-234-5678")
                    .address(address2)
                    .birthDate(LocalDate.of(1990, 3, 22))
                    .gender("Female")
                    .profileImage("https://randomuser.me/api/portraits/women/2.jpg")
                    .active(true)
                    .totalVisits(8)
                    .totalSpent(new BigDecimal("620.00"))
                    .lastVisit(LocalDateTime.now().minusDays(10))
                    .registrationDate(LocalDateTime.now().minusMonths(4))
                    .build();


            CustomerNote note1 = CustomerNote.builder()
                    .content("Prefers appointments in the morning")
                    .createdBy("Admin")
                    .createdAt(LocalDateTime.now().minusDays(30))
                    .build();

            CustomerNote note2 = CustomerNote.builder()
                    .content("Allergic to certain products")
                    .createdBy("Admin")
                    .createdAt(LocalDateTime.now().minusDays(25))
                    .build();

            customer1.addNote(note1);
            customer2.addNote(note2);


            customerRepository.save(customer1);
            customerRepository.save(customer2);
        }
    }

    private void initStaffMembers() {
        if (staffRepository.count() == 0) {

            User staffUser1 = User.builder()
                    .firstName("John")
                    .lastName("Smith")
                    .email("john.smith@example.com")
                    .password(passwordEncoder.encode("password"))
                    .phone("555-123-4567")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User staffUser2 = User.builder()
                    .firstName("Sarah")
                    .lastName("Johnson")
                    .email("sarah.johnson@example.com")
                    .password(passwordEncoder.encode("password"))
                    .phone("555-234-5678")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            User staffUser3 = User.builder()
                    .firstName("Michael")
                    .lastName("Brown")
                    .email("michael.brown@example.com")
                    .password(passwordEncoder.encode("password"))
                    .phone("555-345-6789")
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();


            Set<Role> staffRoles = new HashSet<>();
            Role staffRole = roleRepository.findByName(Role.RoleName.ROLE_STAFF)
                    .orElseThrow(() -> new RuntimeException("Staff role not found"));
            staffRoles.add(staffRole);

            staffUser1.setRoles(staffRoles);
            staffUser2.setRoles(staffRoles);
            staffUser3.setRoles(staffRoles);


            staffUser1 = userRepository.save(staffUser1);
            staffUser2 = userRepository.save(staffUser2);
            staffUser3 = userRepository.save(staffUser3);


            WorkHours[] johnWorkHours = new WorkHours[7];
            WorkHours[] sarahWorkHours = new WorkHours[7];
            WorkHours[] michaelWorkHours = new WorkHours[7];


            for (int i = 0; i < 7; i++) {
                johnWorkHours[i] = WorkHours.builder()
                        .dayOfWeek(i)
                        .isWorking(i > 0 && i < 6)
                        .startTime(i > 0 && i < 6 ? LocalTime.of(9, 0) : null)
                        .endTime(i > 0 && i < 6 ? LocalTime.of(17, 0) : null)
                        .build();

                sarahWorkHours[i] = WorkHours.builder()
                        .dayOfWeek(i)
                        .isWorking(i > 0 && i < 6)
                        .startTime(i > 0 && i < 6 ? LocalTime.of(10, 0) : null)
                        .endTime(i > 0 && i < 6 ? LocalTime.of(18, 0) : null)
                        .build();

                michaelWorkHours[i] = WorkHours.builder()
                        .dayOfWeek(i)
                        .isWorking(i > 0 && i < 5)
                        .startTime(i > 0 && i < 5 ? LocalTime.of(12, 0) : null)
                        .endTime(i > 0 && i < 5 ? LocalTime.of(20, 0) : null)
                        .build();
            }


            TimeSlot johnLunch = TimeSlot.builder()
                    .startTime(LocalTime.of(12, 0))
                    .endTime(LocalTime.of(13, 0))
                    .note("Lunch break")
                    .build();

            TimeSlot sarahLunch = TimeSlot.builder()
                    .startTime(LocalTime.of(13, 0))
                    .endTime(LocalTime.of(14, 0))
                    .note("Lunch break")
                    .build();

            TimeSlot michaelBreak = TimeSlot.builder()
                    .startTime(LocalTime.of(16, 0))
                    .endTime(LocalTime.of(17, 0))
                    .note("Break")
                    .build();


            for (int i = 1; i < 6; i++) {
                johnWorkHours[i].addBreak(TimeSlot.builder()
                        .startTime(LocalTime.of(12, 0))
                        .endTime(LocalTime.of(13, 0))
                        .note("Lunch break")
                        .build());

                sarahWorkHours[i].addBreak(TimeSlot.builder()
                        .startTime(LocalTime.of(13, 0))
                        .endTime(LocalTime.of(14, 0))
                        .note("Lunch break")
                        .build());
            }

            for (int i = 1; i < 5; i++) {
                michaelWorkHours[i].addBreak(TimeSlot.builder()
                        .startTime(LocalTime.of(16, 0))
                        .endTime(LocalTime.of(17, 0))
                        .note("Break")
                        .build());
            }


            Staff staff1 = Staff.builder()
                    .user(staffUser1)
                    .position("Senior Stylist")
                    .specialties(new HashSet<>(Set.of("Haircut", "Coloring", "Styling")))
                    .color("#4CAF50")
                    .avatar("https://randomuser.me/api/portraits/men/32.jpg")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Staff staff2 = Staff.builder()
                    .user(staffUser2)
                    .position("Nail Technician")
                    .specialties(new HashSet<>(Set.of("Manicure", "Pedicure", "Nail Art")))
                    .color("#2196F3")
                    .avatar("https://randomuser.me/api/portraits/women/44.jpg")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Staff staff3 = Staff.builder()
                    .user(staffUser3)
                    .position("Massage Therapist")
                    .specialties(new HashSet<>(Set.of("Swedish Massage", "Deep Tissue", "Hot Stone")))
                    .color("#FF9800")
                    .avatar("https://randomuser.me/api/portraits/men/67.jpg")
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();


            for (int i = 0; i < 7; i++) {
                staff1.addWorkHours(johnWorkHours[i]);
                staff2.addWorkHours(sarahWorkHours[i]);
                staff3.addWorkHours(michaelWorkHours[i]);
            }


            staffRepository.save(staff1);
            staffRepository.save(staff2);
            staffRepository.save(staff3);
        }
    }

    private void initAppointments() {
        if (appointmentRepository.count() == 0) {

            Customer customer1 = customerRepository.findByEmail("john.doe@example.com")
                    .orElseThrow(() -> new RuntimeException("Customer not found"));
            Customer customer2 = customerRepository.findByEmail("jane.smith@example.com")
                    .orElseThrow(() -> new RuntimeException("Customer not found"));


            Service haircut = serviceRepository.findByName("Pedicure")
                    .orElseThrow(() -> new RuntimeException("Service not found"));
            Service manicure = serviceRepository.findByName("Manicure")
                    .orElseThrow(() -> new RuntimeException("Service not found"));


            Staff staff1 = staffRepository.findByUserEmail("john.smith@example.com")
                    .orElseThrow(() -> new RuntimeException("Staff not found"));
            Staff staff2 = staffRepository.findByUserEmail("sarah.johnson@example.com")
                    .orElseThrow(() -> new RuntimeException("Staff not found"));


            Appointment appointment1 = Appointment.builder()
                    .customer(customer1)
                    .service(haircut)
                    .staff(staff1)
                    .date(LocalDate.now().plusDays(1))
                    .time(LocalTime.of(10, 0))
                    .status(Appointment.AppointmentStatus.CONFIRMED)
                    .price(haircut.getPrice())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Appointment appointment2 = Appointment.builder()
                    .customer(customer2)
                    .service(manicure)
                    .staff(staff2)
                    .date(LocalDate.now().plusDays(2))
                    .time(LocalTime.of(11, 0))
                    .status(Appointment.AppointmentStatus.PENDING)
                    .price(manicure.getPrice())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();


            appointmentRepository.save(appointment1);
            appointmentRepository.save(appointment2);
        }
    }
}