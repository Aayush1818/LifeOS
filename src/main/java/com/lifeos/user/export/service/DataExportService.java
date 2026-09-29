package com.lifeos.user.export.service;

import com.lifeos.user.export.dto.LifeOSUserExportDto;

import java.util.UUID;

public interface DataExportService {
    LifeOSUserExportDto exportUserData(UUID userId);
}
