package com.skala.chip.order.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 관리 화면 응답 DTO 모음.
 *
 * 목록(OrderList)과 상세(OrderDetail)를 nested static class 로 묶어 둔다.
 */
public class OrderResponseDTO {

    /**
     * 주문 목록 응답. 상단 배너(납기 임박 N건)와 탭 필터 카운트를 함께 내려준다.
     */
    @Getter
    @Builder
    public static class OrderList {

        private int totalCount;        // 응답에 포함된 주문 수(필터 적용 후)
        private long imminentCount;    // dueImminent == true 주문 수
        private List<OrderSummary> orders;
    }

    /**
     * 목록 행 1건.
     */
    @Getter
    @Builder
    public static class OrderSummary {

        private String orderId;
        private String districtId;
        private String districtName;

        private LocalDate planDate;
        private LocalDateTime dueDate;
        private Integer plannedOutputQty;

        private Integer priority;        // 1~5
        private String priorityLabel;    // 매우 높음 / 높음 / 보통 / 낮음 / 매우 낮음

        private String status;           // 대기 / 진행중 / 완료 (유닛 집계로 유도)
        private int totalUnits;
        private int completedUnits;
        private double progressRatio;    // completedUnits / totalUnits

        private boolean dueImminent;     // 시뮬 기준 오늘 납기 여부 (임박)
        private boolean urgent;          // is_burst (긴급)
    }

    /**
     * 주문 상세 응답. 우측 패널(유닛별 STEP 타임라인)용.
     */
    @Getter
    @Builder
    public static class OrderDetail {

        private String orderId;
        private String districtId;
        private String districtName;

        private LocalDate planDate;
        private LocalDateTime dueDate;
        private Integer plannedOutputQty;

        private Integer priority;
        private String priorityLabel;

        private String status;
        private int totalUnits;
        private int completedUnits;
        private double progressRatio;

        private boolean dueImminent;
        private boolean urgent;

        private List<UnitProgress> units;
    }

    /**
     * 유닛 1개의 진행 상황 + STEP 타임라인.
     */
    @Getter
    @Builder
    public static class UnitProgress {

        private String unitId;
        private Integer unitSizeQty;
        private String unitStatus;
        private LocalDateTime actualStartTime;
        private LocalDateTime actualCompleteTime;

        private String currentStepId;            // 현재 위치 step
        private String currentMachineId;         // 현재 위치 장비
        private LocalDateTime estimatedCompleteTime; // 현재 step 완료 예상 시각

        private List<StepProgress> steps;
    }

    /**
     * STEP 한 단계 상태.
     */
    @Getter
    @Builder
    public static class StepProgress {

        private String stepId;
        private String processStep;   // STEP_A ~ STEP_D
        private Integer stepOrder;
        private String stepStatus;    // done / current / pending
    }
}
