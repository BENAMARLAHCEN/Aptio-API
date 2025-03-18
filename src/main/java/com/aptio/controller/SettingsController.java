// src/main/java/com/aptio/controller/SettingsController.java
package com.aptio.controller;

import com.aptio.dto.BusinessSettingsDTO;
import com.aptio.model.BusinessSettings;
import com.aptio.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping("/business")
    public ResponseEntity<BusinessSettingsDTO> getBusinessSettings() {
        BusinessSettings settings = settingsService.getBusinessSettings();
        return ResponseEntity.ok(BusinessSettingsDTO.fromEntity(settings));
    }

    @PutMapping("/business")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<BusinessSettingsDTO> updateBusinessSettings(@Valid @RequestBody BusinessSettingsDTO settingsDTO) {
        BusinessSettings settings = settingsDTO.toEntity();
        BusinessSettings updatedSettings = settingsService.updateBusinessSettings(settings);
        return ResponseEntity.ok(BusinessSettingsDTO.fromEntity(updatedSettings));
    }
}