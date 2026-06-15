package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;

/**
 * 지연 예측 시스템 상태 (대시보드용).
 *
 * @param status                 SUCCESS / SKIPPED_INSUFFICIENT / FAILED / NONE
 * @param message                상태 설명(입력 부족·추론 실패 사유 등). 정상이면 null.
 * @param insertedCount          마지막 예측에서 새로 기록된 위험 수(성공 시). 실패/미시도 시 null.
 * @param lastAttemptAt          마지막 예측 시도 시각(실제 시각). 없으면 null.
 * @param latestRiskDetectionTime delay_risk 최신 탐지 시각(예측 산출물 신선도, sim 시각). 없으면 null.
 */
public record PredictionStatusResponse(
        String status,
        String message,
        Integer insertedCount,
        LocalDateTime lastAttemptAt,
        LocalDateTime latestRiskDetectionTime
) {}
