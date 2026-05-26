package com.skala.chip.queue.dto;

import java.time.LocalDateTime;

public record QueueResponse(
        String queueId,
        String stepId,
        String processStep,
        String districtId,
        String unitId,
        Integer queuePosition,
        LocalDateTime enqueueTime,
        Double riskScore,
        String riskLevel,
        String riskFactor
) {
}