package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;

/**
 * unit별 대표로 선정된 delay_risk 한 건.
 */
public record RepresentativeRisk(
        String riskId,
        String unitId,
        String stepId,
        String districtId,
        Double riskScore,
        String riskLevel,
        String riskFactor,
        Double estimatedDelayHr,
        Double delayProbability,
        LocalDateTime detectionTime
) {}
