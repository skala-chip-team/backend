package com.skala.chip.monitoring.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

public class MachineRequestDTO {

    @Getter
    @NoArgsConstructor
    public static class UpdateStatusRequest {
        private String machineStatus;
    }
}