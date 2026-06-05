package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;

/**
 * 재조정 그룹에 포함된 delay_risk 상세 (상세 페이지 "관련된 delay_risks" 영역).
 */
public record RelatedRisk(
        String riskId,
        String unitId,
        String riskLevel,
        String riskFactor,
        Double riskScore,
        Double delayProbability,
        Double estimatedDelayHr,
        LocalDateTime detectionTime
) {}
