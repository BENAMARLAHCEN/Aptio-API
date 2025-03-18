package com.aptio.service;

import com.aptio.dto.StaffDTO;
import com.aptio.dto.WorkHoursDTO;

import java.util.List;

public interface StaffService {

    List<StaffDTO> getAllStaff();

    List<StaffDTO> getActiveStaff();

    StaffDTO getStaffById(String id);

    List<StaffDTO> getStaffBySpecialty(String specialty);

    StaffDTO createStaff(StaffDTO staffDTO);

    StaffDTO updateStaff(String id, StaffDTO staffDTO);

    void deleteStaff(String id);

    StaffDTO toggleStaffStatus(String id, boolean active);

    StaffDTO updateWorkHours(String id, List<WorkHoursDTO> workHoursDTO);
}