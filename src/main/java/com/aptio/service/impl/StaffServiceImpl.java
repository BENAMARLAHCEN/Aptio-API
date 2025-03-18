package com.aptio.service.impl;

import com.aptio.dto.StaffDTO;
import com.aptio.dto.TimeSlotDTO;
import com.aptio.dto.WorkHoursDTO;
import com.aptio.exception.ResourceNotFoundException;
import com.aptio.exception.ValidationException;
import com.aptio.model.*;
import com.aptio.repository.RoleRepository;
import com.aptio.repository.StaffRepository;
import com.aptio.repository.UserRepository;
import com.aptio.service.StaffService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    public List<StaffDTO> getAllStaff() {
        return staffRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<StaffDTO> getActiveStaff() {
        return staffRepository.findByIsActive(true).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public StaffDTO getStaffById(String id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));
        return convertToDTO(staff);
    }

    public List<StaffDTO> getStaffBySpecialty(String specialty) {
        return staffRepository.findBySpecialty(specialty).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public StaffDTO createStaff(StaffDTO staffDTO) {
        if (userRepository.existsByEmail(staffDTO.getEmail())) {
            throw new ValidationException("Email is already in use");
        }
        User user = User.builder()
                .firstName(staffDTO.getFirstName())
                .lastName(staffDTO.getLastName())
                .email(staffDTO.getEmail())
                .phone(staffDTO.getPhone())
                .password(passwordEncoder.encode("password"))
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Set<Role> roles = new HashSet<>();
        Role staffRole = roleRepository.findByName(Role.RoleName.ROLE_STAFF)
                .orElseThrow(() -> new RuntimeException("Staff role not found"));
        roles.add(staffRole);
        user.setRoles(roles);
        user = userRepository.save(user);
        Staff staff = Staff.builder()
                .user(user)
                .position(staffDTO.getPosition())
                .specialties(staffDTO.getSpecialties() != null ? new HashSet<>(staffDTO.getSpecialties()) : new HashSet<>())
                .color(staffDTO.getColor())
                .avatar(staffDTO.getAvatar())
                .isActive(staffDTO.getIsActive())
                .workHours(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        if (staffDTO.getWorkHours() != null) {
            for (WorkHoursDTO workHoursDTO : staffDTO.getWorkHours()) {
                WorkHours workHours = convertToWorkHoursEntity(workHoursDTO);
                staff.addWorkHours(workHours);
            }
        } else {
            for (int i = 0; i < 7; i++) {
                WorkHours workHours = WorkHours.builder()
                        .dayOfWeek(i)
                        .isWorking(i > 0 && i < 6)
                        .startTime(i > 0 && i < 6 ? java.time.LocalTime.of(9, 0) : null)
                        .endTime(i > 0 && i < 6 ? java.time.LocalTime.of(17, 0) : null)
                        .breaks(new ArrayList<>())
                        .build();
                if (i > 0 && i < 6) {
                    TimeSlot lunchBreak = TimeSlot.builder()
                            .startTime(java.time.LocalTime.of(12, 0))
                            .endTime(java.time.LocalTime.of(13, 0))
                            .note("Lunch break")
                            .build();
                    workHours.addBreak(lunchBreak);
                }

                staff.addWorkHours(workHours);
            }
        }

        Staff savedStaff = staffRepository.save(staff);
        return convertToDTO(savedStaff);
    }

    @Transactional
    public StaffDTO updateStaff(String id, StaffDTO staffDTO) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));

        User user = staff.getUser();
        if (!user.getEmail().equals(staffDTO.getEmail()) &&
                userRepository.existsByEmail(staffDTO.getEmail())) {
            throw new ValidationException("Email is already in use");
        }
        user.setFirstName(staffDTO.getFirstName());
        user.setLastName(staffDTO.getLastName());
        user.setEmail(staffDTO.getEmail());
        user.setPhone(staffDTO.getPhone());

        userRepository.save(user);
        staff.setPosition(staffDTO.getPosition());

        if (staffDTO.getSpecialties() != null) {
            staff.setSpecialties(new HashSet<>(staffDTO.getSpecialties()));
        }

        staff.setColor(staffDTO.getColor());
        staff.setAvatar(staffDTO.getAvatar());
        staff.setActive(staffDTO.getIsActive());
        if (staffDTO.getWorkHours() != null) {
            Map<Integer, WorkHours> existingWorkHoursMap = staff.getWorkHours().stream()
                    .collect(Collectors.toMap(WorkHours::getDayOfWeek, wh -> wh));

            List<WorkHours> updatedWorkHours = new ArrayList<>();

            for (WorkHoursDTO workHoursDTO : staffDTO.getWorkHours()) {
                WorkHours existingWorkHours = existingWorkHoursMap.get(workHoursDTO.getDayOfWeek());

                if (existingWorkHours != null) {
                    existingWorkHours.setWorking(workHoursDTO.getIsWorking());
                    existingWorkHours.setStartTime(workHoursDTO.getStartTime());
                    existingWorkHours.setEndTime(workHoursDTO.getEndTime());
                    if (workHoursDTO.getBreaks() != null) {
                        existingWorkHours.getBreaks().clear();
                        for (TimeSlotDTO breakDTO : workHoursDTO.getBreaks()) {
                            TimeSlot breakSlot = TimeSlot.builder()
                                    .startTime(breakDTO.getStartTime())
                                    .endTime(breakDTO.getEndTime())
                                    .note(breakDTO.getNote())
                                    .build();

                            existingWorkHours.addBreak(breakSlot);
                        }
                    }

                    updatedWorkHours.add(existingWorkHours);
                } else {
                    WorkHours newWorkHours = convertToWorkHoursEntity(workHoursDTO);
                    newWorkHours.setStaff(staff);
                    updatedWorkHours.add(newWorkHours);
                }
            }
            staff.getWorkHours().clear();
            for (WorkHours workHours : updatedWorkHours) {
                staff.addWorkHours(workHours);
            }
        }

        Staff updatedStaff = staffRepository.save(staff);
        return convertToDTO(updatedStaff);
    }

    @Transactional
    public void deleteStaff(String id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));
        staffRepository.delete(staff);
        userRepository.delete(staff.getUser());
    }

    @Transactional
    public StaffDTO toggleStaffStatus(String id, boolean active) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));

        staff.setActive(active);
        User user = staff.getUser();
        user.setActive(active);
        userRepository.save(user);

        Staff updatedStaff = staffRepository.save(staff);
        return convertToDTO(updatedStaff);
    }

    private StaffDTO convertToDTO(Staff staff) {
        User user = staff.getUser();

        StaffDTO dto = new StaffDTO();
        dto.setId(staff.getId());
        dto.setUserId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setPosition(staff.getPosition());
        dto.setSpecialties(staff.getSpecialties());
        dto.setColor(staff.getColor());
        dto.setAvatar(staff.getAvatar());
        dto.setIsActive(staff.isActive());
        List<WorkHoursDTO> workHoursDTO = staff.getWorkHours().stream()
                .map(this::convertToWorkHoursDTO)
                .collect(Collectors.toList());

        dto.setWorkHours(workHoursDTO);

        return dto;
    }

    private WorkHoursDTO convertToWorkHoursDTO(WorkHours workHours) {
        WorkHoursDTO dto = new WorkHoursDTO();
        dto.setId(workHours.getId());
        dto.setDayOfWeek(workHours.getDayOfWeek());
        dto.setIsWorking(workHours.isWorking());
        dto.setStartTime(workHours.getStartTime());
        dto.setEndTime(workHours.getEndTime());
        List<TimeSlotDTO> breaksDTO = workHours.getBreaks().stream()
                .map(this::convertToTimeSlotDTO)
                .collect(Collectors.toList());

        dto.setBreaks(breaksDTO);

        return dto;
    }

    private TimeSlotDTO convertToTimeSlotDTO(TimeSlot timeSlot) {
        TimeSlotDTO dto = new TimeSlotDTO();
        dto.setId(timeSlot.getId());
        dto.setStartTime(timeSlot.getStartTime());
        dto.setEndTime(timeSlot.getEndTime());
        dto.setNote(timeSlot.getNote());
        return dto;
    }

    private WorkHours convertToWorkHoursEntity(WorkHoursDTO dto) {
        WorkHours workHours = new WorkHours();
        workHours.setDayOfWeek(dto.getDayOfWeek());
        workHours.setWorking(dto.getIsWorking());
        workHours.setStartTime(dto.getStartTime());
        workHours.setEndTime(dto.getEndTime());
        if (dto.getBreaks() != null) {
            for (TimeSlotDTO breakDTO : dto.getBreaks()) {
                TimeSlot breakSlot = TimeSlot.builder()
                        .startTime(breakDTO.getStartTime())
                        .endTime(breakDTO.getEndTime())
                        .note(breakDTO.getNote())
                        .build();

                workHours.addBreak(breakSlot);
            }
        }

        return workHours;
    }

    @Transactional
    public StaffDTO updateWorkHours(String id, List<WorkHoursDTO> workHoursDTO) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "id", id));
        Map<Integer, WorkHours> existingWorkHoursMap = staff.getWorkHours().stream()
                .collect(Collectors.toMap(WorkHours::getDayOfWeek, wh -> wh));

        List<WorkHours> updatedWorkHours = new ArrayList<>();

        for (WorkHoursDTO hoursDTO : workHoursDTO) {
            WorkHours existingWorkHours = existingWorkHoursMap.get(hoursDTO.getDayOfWeek());

            if (existingWorkHours != null) {
                existingWorkHours.setWorking(hoursDTO.getIsWorking());
                existingWorkHours.setStartTime(hoursDTO.getStartTime());
                existingWorkHours.setEndTime(hoursDTO.getEndTime());
                if (hoursDTO.getBreaks() != null) {
                    existingWorkHours.getBreaks().clear();
                    for (TimeSlotDTO breakDTO : hoursDTO.getBreaks()) {
                        TimeSlot breakSlot = TimeSlot.builder()
                                .startTime(breakDTO.getStartTime())
                                .endTime(breakDTO.getEndTime())
                                .note(breakDTO.getNote())
                                .build();

                        existingWorkHours.addBreak(breakSlot);
                    }
                }

                updatedWorkHours.add(existingWorkHours);
            } else {
                WorkHours newWorkHours = convertToWorkHoursEntity(hoursDTO);
                newWorkHours.setStaff(staff);
                updatedWorkHours.add(newWorkHours);
            }
        }
        staff.getWorkHours().clear();
        for (WorkHours workHours : updatedWorkHours) {
            staff.addWorkHours(workHours);
        }

        Staff updatedStaff = staffRepository.save(staff);
        return convertToDTO(updatedStaff);
    }
}