package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;

/**
 * 재조정 전략 선택 확정 결과.
 */
public record RescheduleSelectionResponse(
        String selectionId,
        String groupId,
        String strategy,
        String status,
        LocalDateTime selectedAt,
        String groupStatus
) {}
