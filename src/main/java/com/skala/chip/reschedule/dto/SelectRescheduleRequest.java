package com.skala.chip.reschedule.dto;

import java.util.Map;

/**
 * 재조정 전략 선택 요청.
 * detail 에는 선택된 전략 상세(after_schedule, queue_reorder, metrics_comparison 등)를 통째로 담는다.
 */
public record SelectRescheduleRequest(
        String strategy,
        String recommendation,
        Map<String, Object> detail
) {}
