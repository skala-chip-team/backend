package com.skala.chip.monitoring.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 생산 완료 현황 (생산 완료 알림용).
 *
 * @param completedTodayQty   금일(sim 기준) 전체 구역 완성품 수(최종 공정 output 합)
 * @param latestCompletionAt  최근 완성 작업 시작 시각(sim). 없으면 null.
 * @param planDate            sim 기준 오늘 날짜 (일 경계 리셋 판별용)
 */
public record ProductionStatusResponseDTO(
        long completedTodayQty,
        LocalDateTime latestCompletionAt,
        LocalDate planDate
) {}
