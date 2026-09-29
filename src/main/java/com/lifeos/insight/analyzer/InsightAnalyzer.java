package com.lifeos.insight.analyzer;

import com.lifeos.insight.entity.InsightEntity;
import com.lifeos.user.entity.UserEntity;

import java.util.List;

public interface InsightAnalyzer {
    List<InsightEntity> analyze(UserEntity user);
}
