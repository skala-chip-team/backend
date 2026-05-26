package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;

public record RescheduledQueueItem(
        String queueId,
        String unitId,
        String stepId,
        String districtId,
        Integer beforePosition,
        Integer afterPosition,
        Double score,
        LocalDateTime enqueueTime,
        String reason
) {
}