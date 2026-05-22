package com.skala.chip.monitoring.dto;

import com.skala.chip.monitoring.domain.ScheduleMaster;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class ScheduleResponseDTO {

    @Getter
    @Builder
    public static class ScheduleInfo {

        private String scheduleId;

        private String queueId;

        private String unitId;
        private String unitStatus;

        private String machineId;
        private String machineStatus;

        private String stepId;

        private Integer priority;

        private LocalDateTime estimatedStart;

        private String status;

        private Boolean active;

        public static ScheduleInfo from(ScheduleMaster schedule) {

            return ScheduleInfo.builder()
                    .scheduleId(schedule.getScheduleId())

                    .queueId(schedule.getQueueId())

                    .unitId(
                            schedule.getUnit() != null
                                    ? schedule.getUnit().getUnitId()
                                    : null
                    )
                    .unitStatus(
                            schedule.getUnit() != null
                                    ? schedule.getUnit().getUnitStatus()
                                    : null
                    )

                    .machineId(
                            schedule.getMachine() != null
                                    ? schedule.getMachine().getMachineId()
                                    : null
                    )
                    .machineStatus(
                            schedule.getMachine() != null
                                    ? schedule.getMachine().getMachineStatus()
                                    : null
                    )

                    .stepId(schedule.getStepId())

                    .priority(schedule.getPriority())

                    .estimatedStart(schedule.getEstimatedStart())

                    .status(schedule.getStatus())

                    .active(schedule.getActive())

                    .build();
        }
    }
}