package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전체 대시보드 전용 응답.
 * 한 번의 시뮬 스냅샷(SimClock.now())으로 모든 구역을 원자적으로 반환한다.
 * 구역마다 summary/machines/stepQueues/reschedule 를 따로 폴링해 시점이 어긋나던 문제를 없앤다.
 *
 * 일관성 보장: summary.totalMachineCount == machines.size() (machines 를 machine_master 기준
 * 1기계=1행 으로 구성), machineId 집합/stepOrder 는 폴링 간 안정적이다.
 */
public class MonitoringOverviewResponseDTO {

    @Getter
    @Builder
    public static class DistrictOverview {
        private String districtId;
        private String districtName;
        private Summary summary;
        private List<Machine> machines;
        private List<StepQueue> stepQueues;
        private long rescheduleGroupCount;          // 진행중(pending+approved) 재조정 그룹 수
        private LatestReschedule latestReschedule;  // 최신 재조정 (없으면 null)
    }

    @Getter
    @Builder
    public static class Summary {
        private long totalMachineCount;       // == machines.size()
        private long availableMachineCount;   // 전체 - 정지 - 점검중
        private long downMachineCount;        // 정지
        private double avgUtilizationRate;    // 가동중 / 전체 (%)
        private long totalWaitingUnitCount;
        private double avgWaitTimeMin;
        private long dailyOutputQty;
    }

    @Getter
    @Builder
    public static class Machine {
        private String machineId;
        private String machineStatus;         // 가동 / 점검중 / 정지
        private String processStep;           // 대표 공정 단계명 (step_order 최소)
        private Integer stepOrder;
        private double utilizationRate;       // 금일 가동률 (%)
        private String activeUnitId;          // 현재 작업중 unit (없으면 null)
        // 3D: 장애 지속시간 — 정지 상태일 때 최근 '정지' 이벤트 시작 시각 (아니면 null)
        private LocalDateTime faultSince;
        private LocalDateTime recoveryEta;    // 목표 복구 시각 (현재 데이터 없음 → 항상 null)
    }

    @Getter
    @Builder
    public static class StepQueue {
        private String processStep;
        private Integer stepOrder;
        private long waitingUnitCount;
    }

    @Getter
    @Builder
    public static class LatestReschedule {
        private String groupId;
        private String processStep;
        private Double maxRiskScore;
        private LocalDateTime occurredAt;       // reschedule_group.acted_at
        private String rootCauseCategory;       // reschedule_detail.risk_analysis.root_cause.category
        private List<String> affectedUnits;     // 영향 unit id 목록
        private List<RiskItem> delayRisks;
    }

    @Getter
    @Builder
    public static class RiskItem {
        private String riskId;
        private String riskLevel;
        private LocalDateTime detectionTime;
        private Double estimatedDelayHr;
        private Double delayProbability;
        private String riskFactor;
        private String unitId;
        private String machineId;               // schedule_master(unit_id, step_id)에서 유추, 없으면 null
    }
}
