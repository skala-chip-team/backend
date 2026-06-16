package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 재조정 상세(제안) 페이지 응답.
 *
 * - 그룹 정보 + 관련 delay_risks 는 DB 에서 조립한다.
 * - riskAnalysis 는 에이전트 원인분석(root_cause/evidence/causal_chain ...)을 그대로 전달한다 (없으면 null).
 * - options 는 전략별 재조정안을 평탄화한 카드 목록이다 (에이전트 미호출/실패 시 빈 리스트).
 */
public record RescheduleGroupDetailResponse(
        // 재조정안 그룹 정보
        String groupId,
        String districtId,
        String stepId,
        String processStep,
        Integer stepOrder,
        Double maxDelayProbability,   // reschedule_group.max_risk_score (delay_probability 최대값)
        String groupStatus,           // pending / approved / expired
        LocalDateTime actedAt,
        // 시뮬레이션 스냅샷 시각(sim 기준). 프론트가 actedAt 과의 시점 차이/재시뮬 권장 판단에 사용.
        LocalDateTime simulatedAt,

        // 관련된 delay_risks
        List<RelatedRisk> delayRisks,

        // 에이전트 원인분석 (risk_analysis). 에이전트 미호출 시 null.
        Map<String, Object> riskAnalysis,

        // 적용 전 스케줄 (supervisor_payload.before_schedule). 옵션별 afterSchedule 와 비교용. 없으면 null.
        // 구조: { units: [ { unit_id, steps: [ { step_id, start, finish, machine_id } ] } ] }
        Object beforeSchedule,

        // 전략별 재조정안 카드. 에이전트 미호출/실패 시 빈 리스트.
        List<RescheduleOption> options
) {}
