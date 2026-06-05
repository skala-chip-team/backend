package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;

/**
 * 재조정 전략 선택 확정 결과.
 */
public record SelectRescheduleResponse(
        String groupId,
        String strategy,
        String groupStatus,
        LocalDateTime actedAt
) {}
