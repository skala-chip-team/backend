package com.skala.chip.reschedule.dto;

import java.util.List;

/**
 * 기간별 재조정 이력 조회 응답 (페이지네이션).
 *
 * @param content       해당 페이지의 재조정 이력 항목
 * @param page          현재 페이지 번호(0-base)
 * @param size          페이지 크기
 * @param totalElements 전체 건수
 * @param totalPages    전체 페이지 수
 */
public record RescheduleHistoryResponse(
        List<RescheduleGroupSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
