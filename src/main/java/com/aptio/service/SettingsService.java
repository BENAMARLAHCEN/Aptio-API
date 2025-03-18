package com.aptio.service;

import com.aptio.model.BusinessSettings;

public interface SettingsService {

    BusinessSettings getBusinessSettings();

    BusinessSettings updateBusinessSettings(BusinessSettings settings);
}