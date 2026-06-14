package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class DistrictMachineResponseDTO {

    @Getter
    @Builder
    public static class DistrictMachines {
        private String districtId;
        private String districtName;
        private List<MachineDetail> machines;
    }

    @Getter
    @Builder
    public static class MachineDetail {
        private String machineId;
        private String machineType;
        private String machineStatus;
        private String stepId;
        private String processStep;
        private Double utilizationRate;   // 가동률(%) = 금일 가동 시간 / 경과 시간
        private Double loadRate;          // 부하율(%) = 금일 (완료+진행중) 유닛 / daily_capacity. 가동률과 다름
        private ActiveSchedule activeSchedule;
    }

    @Getter
    @Builder
    public static class ActiveSchedule {
        private String scheduleId;
        private String unitId;
        private LocalDateTime startTime;
        private LocalDateTime estimatedEnd;
        private Integer priority;
    }
}
