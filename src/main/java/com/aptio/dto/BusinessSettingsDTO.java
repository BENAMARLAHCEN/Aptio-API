package com.aptio.dto;

import com.aptio.model.BusinessSettings;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSettingsDTO {
    private Long id;

    @NotBlank(message = "Business name is required")
    private String businessName;

    @NotNull(message = "Business hours start is required")
    private String businessHoursStart;

    @NotNull(message = "Business hours end is required")
    private String businessHoursEnd;

    @NotBlank(message = "Days open is required")
    @Pattern(regexp = "[01]{7}", message = "Days open must be a 7-character string of 0's and 1's")
    private String daysOpen;

    @NotNull(message = "Default appointment duration is required")
    @Min(value = 5, message = "Default appointment duration must be at least 5 minutes")
    private Integer defaultAppointmentDuration;

    @NotNull(message = "Time slot interval is required")
    @Min(value = 5, message = "Time slot interval must be at least 5 minutes")
    private Integer timeSlotInterval;

    @NotNull(message = "Allow overlapping appointments flag is required")
    private Boolean allowOverlappingAppointments;

    @NotNull(message = "Buffer time between appointments is required")
    @Min(value = 0, message = "Buffer time cannot be negative")
    private Integer bufferTimeBetweenAppointments;

    private String address;
    private String phone;
    private String email;
    private String website;

    public static BusinessSettingsDTO fromEntity(BusinessSettings settings) {
        return BusinessSettingsDTO.builder()
                .id(settings.getId())
                .businessName(settings.getBusinessName())
                .businessHoursStart(settings.getBusinessHoursStart().toString())
                .businessHoursEnd(settings.getBusinessHoursEnd().toString())
                .daysOpen(settings.getDaysOpen())
                .defaultAppointmentDuration(settings.getDefaultAppointmentDuration())
                .timeSlotInterval(settings.getTimeSlotInterval())
                .allowOverlappingAppointments(settings.isAllowOverlappingAppointments())
                .bufferTimeBetweenAppointments(settings.getBufferTimeBetweenAppointments())
                .address(settings.getAddress())
                .phone(settings.getPhone())
                .email(settings.getEmail())
                .website(settings.getWebsite())
                .build();
    }

    public BusinessSettings toEntity() {
        return BusinessSettings.builder()
                .id(this.getId())
                .businessName(this.getBusinessName())
                .businessHoursStart(java.time.LocalTime.parse(this.getBusinessHoursStart()))
                .businessHoursEnd(java.time.LocalTime.parse(this.getBusinessHoursEnd()))
                .daysOpen(this.getDaysOpen())
                .defaultAppointmentDuration(this.getDefaultAppointmentDuration())
                .timeSlotInterval(this.getTimeSlotInterval())
                .allowOverlappingAppointments(this.getAllowOverlappingAppointments())
                .bufferTimeBetweenAppointments(this.getBufferTimeBetweenAppointments())
                .address(this.getAddress())
                .phone(this.getPhone())
                .email(this.getEmail())
                .website(this.getWebsite())
                .build();
    }
}