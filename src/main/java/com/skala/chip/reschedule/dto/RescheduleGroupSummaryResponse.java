package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재조정안 관리(목록) 페이지의 항목.
 * 원인(risk_cause_analysis)은 에이전트 출력 의존이라 현재 제외.
 */
public record RescheduleGroupSummaryResponse(
        String groupId,
        String districtId,
        String stepId,
        String processStep,
        Double maxRiskScore,
        String riskLevel,                 // 그룹 내 최고 위험 등급 (Low/Medium/High/Critical)
        String groupStatus,
        LocalDateTime createdAt,          // reschedule_group.acted_at (그룹 생성 시각)
        List<AffectedUnit> affectedUnits  // 영향 unit + 지연 예측 시간
) {}
