package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

public class StatisticsResponseDTO {

    @Getter
    @Builder
    public static class MachineStatistics {
        private long totalMachines;
        private long runningMachines;
        private long idleMachines;
        private long maintenanceMachines;
        private long downMachines;
        private double utilizationRate;
    }
}