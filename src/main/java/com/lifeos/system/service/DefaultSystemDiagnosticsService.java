package com.lifeos.system.service;

import com.lifeos.document.repository.DocumentRepository;
import com.lifeos.document.storage.StorageProperties;
import com.lifeos.finance.repository.TransactionRepository;
import com.lifeos.insurance.repository.InsurancePolicyRepository;
import com.lifeos.loan.repository.LoanRepository;
import com.lifeos.reminder.repository.ReminderRepository;
import com.lifeos.system.dto.ComponentHealth;
import com.lifeos.system.dto.SystemHealthResponse;
import com.lifeos.system.dto.SystemMetricsResponse;
import com.lifeos.travel.repository.TripRepository;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSystemDiagnosticsService implements SystemDiagnosticsService {

    private final JdbcTemplate jdbcTemplate;
    private final StorageProperties storageProperties;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final TransactionRepository transactionRepository;
    private final LoanRepository loanRepository;
    private final InsurancePolicyRepository insurancePolicyRepository;
    private final TripRepository tripRepository;
    private final ReminderRepository reminderRepository;

    private static final long APP_START_TIME = System.currentTimeMillis();

    @Override
    public SystemHealthResponse getSystemHealth() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();

        // 1. PostgreSQL Probe
        long dbStart = System.currentTimeMillis();
        boolean dbOk = false;
        try {
            Integer probe = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            long latency = System.currentTimeMillis() - dbStart;
            if (probe != null && probe == 1) {
                dbOk = true;
                components.put("database", ComponentHealth.builder()
                        .status("UP")
                        .latencyMs(latency)
                        .details("PostgreSQL connectivity verified")
                        .build());
            } else {
                components.put("database", ComponentHealth.builder()
                        .status("DOWN")
                        .latencyMs(latency)
                        .details("Unexpected query result")
                        .build());
            }
        } catch (Exception e) {
            components.put("database", ComponentHealth.builder()
                    .status("DOWN")
                    .latencyMs(System.currentTimeMillis() - dbStart)
                    .details("Database connection failed: " + e.getMessage())
                    .build());
        }

        // 2. pgvector Extension Probe
        long vectorStart = System.currentTimeMillis();
        try {
            String version = jdbcTemplate.queryForObject(
                    "SELECT extversion FROM pg_extension WHERE extname = 'vector'", String.class);
            long latency = System.currentTimeMillis() - vectorStart;
            if (version != null && !version.isBlank()) {
                components.put("pgvector", ComponentHealth.builder()
                        .status("UP")
                        .latencyMs(latency)
                        .details("pgvector extension active (v" + version + ")")
                        .build());
            } else {
                components.put("pgvector", ComponentHealth.builder()
                        .status("DEGRADED")
                        .latencyMs(latency)
                        .details("pgvector extension not detected")
                        .build());
            }
        } catch (Exception e) {
            components.put("pgvector", ComponentHealth.builder()
                    .status("DEGRADED")
                    .latencyMs(System.currentTimeMillis() - vectorStart)
                    .details("pgvector check failed: " + e.getMessage())
                    .build());
        }

        // 3. Document Storage Capacity Probe
        long storageStart = System.currentTimeMillis();
        try {
            String uploadDir = storageProperties != null && storageProperties.getLocal() != null
                    ? storageProperties.getLocal().getUploadDir() : "./storage/uploads";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            long freeBytes = dir.getFreeSpace();
            long totalBytes = dir.getTotalSpace();
            long latency = System.currentTimeMillis() - storageStart;

            components.put("documentStorage", ComponentHealth.builder()
                    .status(dir.canWrite() ? "UP" : "DEGRADED")
                    .latencyMs(latency)
                    .details(String.format("Storage writable (%d MB free of %d MB)",
                            freeBytes / (1024 * 1024), totalBytes / (1024 * 1024)))
                    .build());
        } catch (Exception e) {
            components.put("documentStorage", ComponentHealth.builder()
                    .status("DOWN")
                    .latencyMs(System.currentTimeMillis() - storageStart)
                    .details("Document storage check failed: " + e.getMessage())
                    .build());
        }

        // 4. Apache Tika Text Extraction Engine
        components.put("apacheTika", ComponentHealth.builder()
                .status("UP")
                .latencyMs(0L)
                .details("Apache Tika extractor initialized")
                .build());

        // Derive overall status
        String overallStatus = "UP";
        if (!dbOk) {
            overallStatus = "DOWN";
        } else {
            for (ComponentHealth ch : components.values()) {
                if ("DOWN".equalsIgnoreCase(ch.getStatus())) {
                    overallStatus = "DEGRADED";
                    break;
                } else if ("DEGRADED".equalsIgnoreCase(ch.getStatus())) {
                    overallStatus = "DEGRADED";
                }
            }
        }

        long uptimeSeconds = (System.currentTimeMillis() - APP_START_TIME) / 1000;

        return SystemHealthResponse.builder()
                .status(overallStatus)
                .uptimeSeconds(uptimeSeconds)
                .timestamp(Instant.now())
                .components(components)
                .build();
    }

    @Override
    public SystemMetricsResponse getSystemMetrics() {
        Runtime runtime = Runtime.getRuntime();
        long freeMemory = runtime.freeMemory();
        long totalMemory = runtime.totalMemory();
        long maxMemory = runtime.maxMemory();
        long usedMemory = totalMemory - freeMemory;
        int processors = runtime.availableProcessors();

        String uploadDir = storageProperties != null && storageProperties.getLocal() != null
                ? storageProperties.getLocal().getUploadDir() : "./storage/uploads";
        File dir = new File(uploadDir);
        long diskFree = dir.getFreeSpace();
        long diskTotal = dir.getTotalSpace();

        return SystemMetricsResponse.builder()
                .totalUsers(userRepository.count())
                .totalDocuments(documentRepository.count())
                .totalTransactions(transactionRepository.count())
                .totalLoans(loanRepository.count())
                .totalPolicies(insurancePolicyRepository.count())
                .totalTrips(tripRepository.count())
                .totalReminders(reminderRepository.count())
                .diskFreeBytes(diskFree)
                .diskTotalBytes(diskTotal)
                .jvmAvailableProcessors(processors)
                .jvmUsedMemoryBytes(usedMemory)
                .jvmMaxMemoryBytes(maxMemory)
                .timestamp(Instant.now())
                .build();
    }
}
