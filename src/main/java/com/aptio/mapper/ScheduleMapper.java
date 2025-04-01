package com.aptio.mapper;

import com.aptio.dto.ScheduleEntryDTO;
import com.aptio.model.Appointment;
import com.aptio.model.ScheduleEntry;
import com.aptio.model.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class ScheduleMapper {

    public ScheduleEntryDTO toDTO(ScheduleEntry entry) {
        if (entry == null) {
            return null;
        }

        ScheduleEntryDTO dto = new ScheduleEntryDTO();
        dto.setId(entry.getId());
        if (entry.getStaff() != null) {
            dto.setStaffId(entry.getStaff().getId());
            if (entry.getStaff().getUser() != null) {
                dto.setStaffName(entry.getStaff().getUser().getFirstName() + " " +
                        entry.getStaff().getUser().getLastName());
            }
        }
        if (entry.getAppointment() != null) {
            dto.setAppointmentId(entry.getAppointment().getId());
        }
        dto.setTitle(entry.getTitle());
        dto.setDate(entry.getDate());
        dto.setStartTime(entry.getStartTime());
        dto.setEndTime(entry.getEndTime());
        dto.setNotes(entry.getNotes());
        dto.setType(entry.getType().name());
        dto.setStatus(entry.getStatus().name());
        dto.setColor(entry.getColor());

        return dto;
    }

    public ScheduleEntry toEntity(ScheduleEntryDTO dto) {
        if (dto == null) {
            return null;
        }

        ScheduleEntry entry = new ScheduleEntry();
        entry.setTitle(dto.getTitle());
        entry.setDate(dto.getDate());
        entry.setStartTime(dto.getStartTime());
        entry.setEndTime(dto.getEndTime());
        entry.setNotes(dto.getNotes());
        if (dto.getType() != null) {
            entry.setType(ScheduleEntry.EntryType.valueOf(dto.getType()));
        } else {
            entry.setType(ScheduleEntry.EntryType.OTHER);
        }

        if (dto.getStatus() != null) {
            entry.setStatus(ScheduleEntry.EntryStatus.valueOf(dto.getStatus()));
        } else {
            entry.setStatus(ScheduleEntry.EntryStatus.SCHEDULED);
        }

        entry.setColor(dto.getColor());
        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        return entry;
    }

    public void updateEntityFromDTO(ScheduleEntryDTO dto, ScheduleEntry entry) {
        if (dto == null || entry == null) {
            return;
        }

        entry.setTitle(dto.getTitle());
        entry.setDate(dto.getDate());
        entry.setStartTime(dto.getStartTime());
        entry.setEndTime(dto.getEndTime());
        entry.setNotes(dto.getNotes());

        if (dto.getType() != null) {
            entry.setType(ScheduleEntry.EntryType.valueOf(dto.getType()));
        }

        if (dto.getStatus() != null) {
            entry.setStatus(ScheduleEntry.EntryStatus.valueOf(dto.getStatus()));
        }

        entry.setColor(dto.getColor());
        entry.setUpdatedAt(LocalDateTime.now());
    }

    
    public ScheduleEntry createEntryFromAppointment(Appointment appointment, Staff staff) {
        if (appointment == null) {
            return null;
        }

        ScheduleEntry entry = new ScheduleEntry();
        entry.setStaff(staff);
        entry.setAppointment(appointment);
        String title = appointment.getService().getName();
        if (appointment.getCustomer() != null) {
            title += " - " + appointment.getCustomer().getFirstName() + " " +
                    appointment.getCustomer().getLastName();
        }
        entry.setTitle(title);

        entry.setDate(appointment.getDate());
        entry.setStartTime(appointment.getTime());

        entry.setEndTime(appointment.getTime().plusMinutes(appointment.getService().getDuration()));

        entry.setNotes(appointment.getNotes());
        entry.setType(ScheduleEntry.EntryType.APPOINTMENT);

        switch (appointment.getStatus()) {
            case COMPLETED:
                entry.setStatus(ScheduleEntry.EntryStatus.COMPLETED);
                break;
            case CANCELLED:
                entry.setStatus(ScheduleEntry.EntryStatus.CANCELLED);
                break;
            default:
                entry.setStatus(ScheduleEntry.EntryStatus.SCHEDULED);
        }

        entry.setCreatedAt(LocalDateTime.now());
        entry.setUpdatedAt(LocalDateTime.now());

        return entry;
    }

}