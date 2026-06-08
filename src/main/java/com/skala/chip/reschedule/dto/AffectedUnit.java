package com.skala.chip.reschedule.dto;

/**
 * 재조정 그룹에 영향받는 unit 과 지연 예측 시간.
 */
public record AffectedUnit(
        String unitId,
        Double estimatedDelayHr
) {}
