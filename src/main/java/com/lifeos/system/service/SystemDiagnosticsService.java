package com.lifeos.system.service;

import com.lifeos.system.dto.SystemHealthResponse;
import com.lifeos.system.dto.SystemMetricsResponse;

public interface SystemDiagnosticsService {
    SystemHealthResponse getSystemHealth();
    SystemMetricsResponse getSystemMetrics();
}
