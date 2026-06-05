package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class StepQueueResponseDTO {

    /**
     * 구역 단위 step별 큐 정보. step_order 순으로 정렬된 공정 단계 목록을 가진다.
     */
    @Getter
    @Builder
    public static class DistrictStepQueue {

        private String districtId;
        private String districtName;

        private List<StepQueue> steps;
    }

    /**
     * 공정 단계(step)별 큐 요약.
     */
    @Getter
    @Builder
    public static class StepQueue {

        private String stepId;
        private String processStep;   // 공정 단계명
        private Integer stepOrder;    // 공정 순서

        private long waitingUnitCount;   // 대기 unit 수
        private double avgWaitTimeMin;   // 평균 대기 시간 (분)

        // 대기 unit 목록 (queue_position 오름차순 = 대기 순서)
        private List<WaitingUnit> waitingUnits;
    }

    /**
     * 대기 중인 unit 한 개 (큐 항목 1건).
     */
    @Getter
    @Builder
    public static class WaitingUnit {

        private String queueId;

        private String unitId;     // unit 식별자 (이름 역할)
        private String orderId;    // 주문번호 (참고용)
        private String unitStatus;

        private Integer queuePosition;  // 대기 순서
        private LocalDateTime enqueueTime;
        private Double actualWaitTime;
        private String status;
    }
}
