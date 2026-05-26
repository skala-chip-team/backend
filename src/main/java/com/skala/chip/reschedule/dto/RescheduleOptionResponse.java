package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RescheduleOptionResponse(
        String optionId,
        String targetQueueId,
        String perspective,
        String status,
        LocalDateTime createdAt,
        List<RescheduledQueueItem> beforeQueue,
        List<RescheduledQueueItem> afterQueue
) {
}