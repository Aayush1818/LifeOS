package com.lifeos.user.export.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LifeOSUserExportDto {
    private String exportVersion;
    private OffsetDateTime exportedAt;
    private UUID userId;
    private Map<String, Object> userProfile;
    private List<Map<String, Object>> dependents;
    private List<Map<String, Object>> transactions;
    private List<Map<String, Object>> recurringTransactions;
    private List<Map<String, Object>> budgets;
    private List<Map<String, Object>> loans;
    private List<Map<String, Object>> insurancePolicies;
    private List<Map<String, Object>> appointments;
    private List<Map<String, Object>> trips;
    private List<Map<String, Object>> assets;
    private List<Map<String, Object>> reminders;
    private List<Map<String, Object>> notifications;
    private List<Map<String, Object>> documents;
    private List<Map<String, Object>> conversations;
}
