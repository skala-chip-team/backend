package com.skala.chip.monitoring.dto;

import com.skala.chip.monitoring.domain.ProcessQueue;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class QueueResponseDTO {

    @Getter
    @Builder
    public static class QueueInfo {

        private String queueId;

        private String stepId;

        private String districtId;
        private String districtName;

        private String unitId;
        private String unitStatus;

        private Integer queuePosition;

        private LocalDateTime enqueueTime;

        private Double actualWaitTime;

        private String status;

        public static QueueInfo from(ProcessQueue queue) {

            return QueueInfo.builder()
                    .queueId(queue.getQueueId())

                    .stepId(queue.getStepId())

                    .districtId(
                            queue.getDistrict() != null
                                    ? queue.getDistrict().getDistrictId()
                                    : null
                    )
                    .districtName(
                            queue.getDistrict() != null
                                    ? queue.getDistrict().getDistrictName()
                                    : null
                    )

                    .unitId(
                            queue.getUnit() != null
                                    ? queue.getUnit().getUnitId()
                                    : null
                    )
                    .unitStatus(
                            queue.getUnit() != null
                                    ? queue.getUnit().getUnitStatus()
                                    : null
                    )

                    .queuePosition(queue.getQueuePosition())

                    .enqueueTime(queue.getEnqueueTime())

                    .actualWaitTime(queue.getActualWaitTime())

                    .status(queue.getStatus())

                    .build();
        }
    }
}