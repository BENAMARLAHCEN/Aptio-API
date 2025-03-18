package com.aptio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for user appointment requests.
 * Customer ID is omitted as it will be determined from the authenticated user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserAppointmentRequestDTO {

    @NotBlank(message = "Service ID is required")
    private String serviceId;

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Time is required")
    private LocalTime time;

    private String staffId;

    private String notes;
}