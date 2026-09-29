package com.lifeos.insight.service;

import com.lifeos.common.exception.ResourceNotFoundException;
import com.lifeos.insight.analyzer.InsightAnalyzer;
import com.lifeos.insight.dto.InsightResponse;
import com.lifeos.insight.dto.InsightSummaryResponse;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.repository.InsightRepository;
import com.lifeos.user.entity.UserEntity;
import com.lifeos.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultInsightService implements InsightService {

    private final UserRepository userRepository;
    private final InsightRepository insightRepository;
    private final List<InsightAnalyzer> analyzers;

    @Override
    @Transactional
    public InsightSummaryResponse generateInsights(UUID userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        log.info("Executing proactive life analyzers for user [{}] across {} rules", userId, analyzers.size());

        List<InsightEntity> newlyDiscovered = new ArrayList<>();
        for (InsightAnalyzer analyzer : analyzers) {
            try {
                List<InsightEntity> results = analyzer.analyze(user);
                if (results != null) {
                    for (InsightEntity entity : results) {
                        boolean exists = insightRepository
                                .existsByUserIdAndInsightTypeAndTitleAndIsDismissedFalseAndIsDeletedFalse(
                                        userId, entity.getInsightType(), entity.getTitle());
                        if (!exists) {
                            newlyDiscovered.add(entity);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Analyzer [{}] failed for user [{}]: {}",
                        analyzer.getClass().getSimpleName(), userId, e.getMessage(), e);
            }
        }

        if (!newlyDiscovered.isEmpty()) {
            insightRepository.saveAll(newlyDiscovered);
            log.info("Persisted {} new proactive insights for user [{}]", newlyDiscovered.size(), userId);
        }

        return getActiveInsights(userId, null);
    }

    @Override
    @Transactional
    public InsightSummaryResponse getActiveInsights(UUID userId, InsightSeverity minSeverity) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        List<InsightEntity> active = insightRepository
                .findAllByUserIdAndIsDismissedFalseAndIsDeletedFalseOrderByCreatedAtDesc(userId);

        if (active.isEmpty()) {
            // First-time or on-demand auto-generation
            UserEntity user = userRepository.findById(userId).orElseThrow();
            List<InsightEntity> generated = new ArrayList<>();
            for (InsightAnalyzer analyzer : analyzers) {
                try {
                    List<InsightEntity> res = analyzer.analyze(user);
                    if (res != null) {
                        generated.addAll(res);
                    }
                } catch (Exception e) {
                    log.warn("Analyzer execution error during fallback generation: {}", e.getMessage());
                }
            }
            if (!generated.isEmpty()) {
                active = insightRepository.saveAll(generated);
            }
        }

        List<InsightResponse> responseList = new ArrayList<>();
        long critical = 0;
        long warning = 0;
        long info = 0;

        for (InsightEntity entity : active) {
            if (entity.getSeverity() == InsightSeverity.CRITICAL) critical++;
            else if (entity.getSeverity() == InsightSeverity.WARNING) warning++;
            else info++;

            if (minSeverity == null || isAtLeastSeverity(entity.getSeverity(), minSeverity)) {
                responseList.add(InsightResponse.fromEntity(entity));
            }
        }

        return InsightSummaryResponse.builder()
                .totalActive(active.size())
                .criticalCount(critical)
                .warningCount(warning)
                .infoCount(info)
                .insights(responseList)
                .build();
    }

    @Override
    @Transactional
    public InsightResponse dismissInsight(UUID userId, UUID insightId) {
        InsightEntity entity = insightRepository.findByIdAndUserIdAndIsDeletedFalse(insightId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Insight not found: " + insightId));

        entity.setDismissed(true);
        InsightEntity updated = insightRepository.save(entity);
        log.info("Dismissed insight [{}] for user [{}]", insightId, userId);
        return InsightResponse.fromEntity(updated);
    }

    @Override
    @Transactional
    public InsightResponse actionInsight(UUID userId, UUID insightId) {
        InsightEntity entity = insightRepository.findByIdAndUserIdAndIsDeletedFalse(insightId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Insight not found: " + insightId));

        entity.setActioned(true);
        InsightEntity updated = insightRepository.save(entity);
        log.info("Actioned insight [{}] for user [{}]", insightId, userId);
        return InsightResponse.fromEntity(updated);
    }

    private boolean isAtLeastSeverity(InsightSeverity actual, InsightSeverity target) {
        if (target == null || target == InsightSeverity.INFO) return true;
        if (target == InsightSeverity.WARNING) {
            return actual == InsightSeverity.WARNING || actual == InsightSeverity.CRITICAL;
        }
        return actual == InsightSeverity.CRITICAL;
    }
}
