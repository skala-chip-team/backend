package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 재조정 상세 페이지 응답.
 *
 * - 그룹 정보 + 관련 delay_risks 는 DB 에서 조립한다.
 * - rescheduleDetail 은 에이전트 출력(jsonb) 을 그대로 전달한다 (현재 미구현이라 null).
 *   에이전트 연동 후 affected_scope / risk_cause_analysis / strategy 목록·상세 등이 채워진다.
 */
public record RescheduleGroupDetailResponse(
        // 재조정안 그룹 정보
        String groupId,
        String districtId,
        String stepId,
        String processStep,
        Integer stepOrder,
        Double maxDelayProbability,   // reschedule_group.max_risk_score (delay_probability 최대값)
        String groupStatus,
        LocalDateTime actedAt,

        // 관련된 delay_risks
        List<RelatedRisk> delayRisks,

        // 에이전트 출력 (affected_scope / risk_cause_analysis / strategies / ... ), 현재 null
        Map<String, Object> rescheduleDetail
) {}
