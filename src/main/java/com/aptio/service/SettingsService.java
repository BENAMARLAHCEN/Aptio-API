// src/main/java/com/aptio/service/SettingsService.java
package com.aptio.service;

import com.aptio.model.BusinessSettings;
import com.aptio.repository.BusinessSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class SettingsService {

    private final BusinessSettingsRepository businessSettingsRepository;

    public BusinessSettings getBusinessSettings() {
        BusinessSettings settings = businessSettingsRepository.findFirstByOrderById();

        if (settings == null) {
            settings = BusinessSettings.builder()
                    .businessName("Aptio Appointment System")
                    .businessHoursStart(LocalTime.of(9, 0))
                    .businessHoursEnd(LocalTime.of(18, 0))
                    .daysOpen("0111110") // Mon-Fri
                    .defaultAppointmentDuration(30)
                    .timeSlotInterval(15)
                    .allowOverlappingAppointments(false)
                    .bufferTimeBetweenAppointments(5)
                    .address("123 Business St, City, State 12345")
                    .phone("555-123-4567")
                    .email("contact@aptio.com")
                    .website("https://aptio.com")
                    .build();

            settings = businessSettingsRepository.save(settings);
        }

        return settings;
    }

    @Transactional
    public BusinessSettings updateBusinessSettings(BusinessSettings settings) {
        BusinessSettings existingSettings = businessSettingsRepository.findFirstByOrderById();

        if (existingSettings == null) {
            return businessSettingsRepository.save(settings);
        }

        existingSettings.setBusinessName(settings.getBusinessName());
        existingSettings.setBusinessHoursStart(settings.getBusinessHoursStart());
        existingSettings.setBusinessHoursEnd(settings.getBusinessHoursEnd());
        existingSettings.setDaysOpen(settings.getDaysOpen());
        existingSettings.setDefaultAppointmentDuration(settings.getDefaultAppointmentDuration());
        existingSettings.setTimeSlotInterval(settings.getTimeSlotInterval());
        existingSettings.setAllowOverlappingAppointments(settings.isAllowOverlappingAppointments());
        existingSettings.setBufferTimeBetweenAppointments(settings.getBufferTimeBetweenAppointments());
        existingSettings.setAddress(settings.getAddress());
        existingSettings.setPhone(settings.getPhone());
        existingSettings.setEmail(settings.getEmail());
        existingSettings.setWebsite(settings.getWebsite());
        existingSettings.setUpdatedAt(java.time.LocalDateTime.now());

        return businessSettingsRepository.save(existingSettings);
    }
}