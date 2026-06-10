package com.skala.chip.reschedule.dto;

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
        Object queueReorder              // [ { unit_id, queue_id, original_queue_position, new_queue_position, priority_score } ]
) {}
