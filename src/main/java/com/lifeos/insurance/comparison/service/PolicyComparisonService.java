package com.lifeos.insurance.comparison.service;

import com.lifeos.insurance.comparison.dto.PolicyComparisonRequest;
import com.lifeos.insurance.comparison.dto.PolicyComparisonResponse;

import java.util.UUID;

public interface PolicyComparisonService {
    PolicyComparisonResponse comparePolicies(UUID userId, PolicyComparisonRequest request);
}
