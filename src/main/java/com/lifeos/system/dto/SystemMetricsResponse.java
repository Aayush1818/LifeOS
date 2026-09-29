package com.lifeos.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemMetricsResponse {
    private long totalUsers;
    private long totalDocuments;
    private long totalTransactions;
    private long totalLoans;
    private long totalPolicies;
    private long totalTrips;
    private long totalReminders;
    private long diskFreeBytes;
    private long diskTotalBytes;
    private int jvmAvailableProcessors;
    private long jvmUsedMemoryBytes;
    private long jvmMaxMemoryBytes;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
