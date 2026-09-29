package com.lifeos.insight.analyzer;

import com.lifeos.asset.entity.WarrantyEntity;
import com.lifeos.asset.entity.WarrantyStatus;
import com.lifeos.asset.repository.WarrantyRepository;
import com.lifeos.insight.entity.InsightActionType;
import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.insight.entity.InsightSeverity;
import com.lifeos.insight.entity.InsightType;
import com.lifeos.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiringWarrantyAnalyzer implements InsightAnalyzer {

    private final WarrantyRepository warrantyRepository;

    @Override
    public List<InsightEntity> analyze(UserEntity user) {
        List<InsightEntity> insights = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate expiryThreshold = now.plusDays(30);

        List<WarrantyEntity> expiringWarranties = warrantyRepository
                .findAllByUserIdAndStatusAndExpiryDateBetweenAndIsDeletedFalse(
                        user.getId(), WarrantyStatus.ACTIVE, now, expiryThreshold);

        for (WarrantyEntity warranty : expiringWarranties) {
            String assetName = warranty.getAsset() != null ? warranty.getAsset().getName() : "Tracked Asset";

            insights.add(InsightEntity.builder()
                    .user(user)
                    .insightType(InsightType.WARRANTY_EXPIRING)
                    .severity(InsightSeverity.INFO)
                    .title("Expiring Warranty: " + assetName)
                    .description(String.format("Warranty for %s from %s expires on %s. Inspect product condition and submit any eligible warranty service claims prior to expiration.",
                            assetName, warranty.getProvider(), warranty.getExpiryDate()))
                    .actionType(InsightActionType.VIEW_WARRANTY)
                    .actionPayload(Map.of(
                            "warrantyId", warranty.getId().toString(),
                            "assetName", assetName,
                            "expiryDate", warranty.getExpiryDate().toString(),
                            "provider", warranty.getProvider()
                    ))
                    .build());
        }

        return insights;
    }
}
