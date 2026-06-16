package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class DistrictSummaryResponseDTO {

    @Getter
    @Builder
    public static class DistrictSummary {

        private String districtId;
        private String districtName;

        // 시뮬레이션 스냅샷 시각(sim 기준). 실시간 상태 갱신 시각 표시·경과시간 계산용.
        private LocalDateTime simulatedAt;

        // 장비 현황
        private long totalMachineCount;      // 전체 장비 수
        private long availableMachineCount;  // 가용 장비 수 (가동중 + 대기)
        private long downMachineCount;       // 장애 장비 수
        private double avgUtilizationRate;   // 평균 가동률 (가동중 / 전체, %)

        // 대기 unit 현황
        private long totalWaitingUnitCount;  // 전체 대기 unit 수
        private double avgWaitTimeMin;       // 평균 대기 시간 (분)

        // 생산 현황
        private long dailyOutputQty;         // 금일 생산량 (실제 STEP 완료 output_qty 합)
        private long dailyTargetOutputQty;   // 금일 생산 목표량 (plan_date=오늘 주문의 planned_output_qty 합)
        // 달성률(%) = dailyOutputQty / dailyTargetOutputQty * 100. 목표가 없으면(0) null = "달성률 산출 불가".
        private Double achievementRate;
    }
}
