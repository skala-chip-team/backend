package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class WorkStatusResponseDTO {

    @Getter
    @Builder
    public static class WorkStatusInfo {

        private String statusId;

        private String scheduleId;

        private String machineId;

        private String machineStatus;

        private String districtId;

        private String unitId;

        private String operatorId;

        private String shift;

        private LocalDateTime startTime;

        private LocalDateTime endTime;

        private Integer defectCount;

        private Integer outputQty;
    }
}