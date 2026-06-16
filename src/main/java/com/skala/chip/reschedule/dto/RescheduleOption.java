package com.skala.chip.reschedule.dto;

import java.util.List;

/**
 * 재조정 상세 페이지의 전략별 재조정안 카드.
 *
 * 에이전트 출력(reschedule_result.reschedule_options[i] + decision_summaries[i])을
 * 프론트가 바로 쓰도록 평탄화한 형태. 깊은 중첩 탐색/교차참조 없이 카드 1개로 렌더링한다.
 * fallback 결과는 metrics/afterSchedule 가 null 일 수 있으므로 analysisStatus 로 구분한다.
 */
public record RescheduleOption(
        String strategy,                  // due_date_first / bottleneck_minimization / utilization_balance
        String analysisStatus,            // success / fallback
        String fallbackReason,            // fallback 사유 (success 면 null)
        boolean recommended,              // decision_summaries.recommendation == "recommend"
        String recommendation,            // recommend / not_recommend (fallback 시 not_recommend 로 강제)
        boolean manualReviewRequired,     // 운영자 수동 검토 대상 여부 (fallback/not_recommend 면 true)
        String summary,                   // 추천 사유 요약 (decision_summaries.summary)
        boolean selected,                 // selected_yn (선택·확정된 전략 여부)

        // 적용 후 시뮬레이션 지표 (dispatch_simulation). 값이 없으면 null.
        Double estimatedDelayHrAfter,
        Double avgWaitTimeMinAfter,
        Double avgUtilizationRateAfter,
        Double maxWaitTimeMinAfter,
        Integer deadlineViolationCount,

        // 적용 대상 원본 (스케줄 미리보기/큐 변경표용). 구조는 에이전트 출력 그대로.
        Object afterSchedule,             // { units: [ { unit_id, steps: [ { step_id, start, finish, machine_id } ] } ] }
        Object queueReorder,             // [ { unit_id, queue_id, original_queue_position, new_queue_position, priority_score } ]

        // 적용 전/후 비교 지표 (decision_summaries.metrics_comparison). 없으면 null.
        MetricsComparison metricsComparison,

        // 추천 근거 (decision_summaries)
        String recommendationReasoning,    // recommendation_reasoning
        List<KeyPoint> keyImprovements,    // key_improvements [{ description, magnitude }]
        List<KeyPoint> keyConcerns,        // key_concerns     [{ description, magnitude, mitigation }]
        DetailedReport detailedReport,     // detailed_report (설명 텍스트)
        DeadlineImpact deadlineImpact      // deadline_impact (납기 영향 수치)
) {

    /**
     * 재조정 적용 전/후 비교. 생산량 차이를 비롯한 핵심 지표의 before/after/delta.
     */
    public record MetricsComparison(
            Delta completedUnits,          // 생산량(완료 unit 수) 차이 — throughput.completed_units
            Delta cumulativeDelayHr,       // 누적 지연(시간) 차이 — delay.cumulative_delay_hr
            Delta avgQueueWaitMin,         // 평균 대기(분) 차이 — delay.avg_queue_wait_min
            Delta deadlineViolationCount,  // 납기 위반 수 차이 — delay.deadline_violation_count
            Delta overallLoad              // 전체 장비 부하율 차이 — load.overall
    ) {}

    /** before/after/delta 한 묶음. */
    public record Delta(Double before, Double after, Double delta) {}

    /** 개선점/우려점 한 항목. mitigation 은 우려점(key_concerns)에만 존재(없으면 null). */
    public record KeyPoint(String description, String magnitude, String mitigation) {}

    /** 상세 리포트 텍스트 (detailed_report). */
    public record DetailedReport(
            String executiveSummary,
            String riskBackground,
            String metricAnalysis,
            String tradeoffs,
            String decisionBasis
    ) {}

    /** 납기 영향 (deadline_impact). */
    public record DeadlineImpact(
            Integer rescuedCount,
            Integer stillAtRiskCount,
            Integer newlyAtRiskCount,
            Integer newlyViolatedCount
    ) {}
}
