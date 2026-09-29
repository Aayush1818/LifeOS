package com.lifeos.insight.repository;

import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InsightRepository extends JpaRepository<InsightEntity, UUID> {

    List<InsightEntity> findAllByUserIdAndIsDismissedFalseAndIsDeletedFalseOrderByCreatedAtDesc(UUID userId);

    List<InsightEntity> findAllByUserIdAndSeverityAndIsDismissedFalseAndIsDeletedFalse(UUID userId, InsightSeverity severity);

    Optional<InsightEntity> findByIdAndUserIdAndIsDeletedFalse(UUID id, UUID userId);

    boolean existsByUserIdAndInsightTypeAndTitleAndIsDismissedFalseAndIsDeletedFalse(UUID userId, InsightType type, String title);

    long countByUserIdAndIsDismissedFalseAndIsDeletedFalse(UUID userId);

    long countByUserIdAndSeverityAndIsDismissedFalseAndIsDeletedFalse(UUID userId, InsightSeverity severity);
}
