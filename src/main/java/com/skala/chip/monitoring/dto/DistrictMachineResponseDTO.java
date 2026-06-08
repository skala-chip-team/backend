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
        private Double utilizationRate;
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
