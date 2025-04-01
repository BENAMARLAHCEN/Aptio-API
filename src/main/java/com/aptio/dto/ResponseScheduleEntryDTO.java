package com.aptio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseScheduleEntryDTO {
    private String id;

    @NotBlank(message = "Staff ID is required")
    private String staffId;

    private String staffName;

    private String appointmentId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Start time is required")
    private String startTime;

    @NotNull(message = "End time is required")
    private String endTime;

    private String notes;

    private String type;

    private String status;

    private String color;
}
