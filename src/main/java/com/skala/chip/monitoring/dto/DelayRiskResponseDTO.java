package com.skala.chip.monitoring.dto;

import com.skala.chip.monitoring.domain.DelayRisk;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class DelayRiskResponseDTO {

    @Getter
    @Builder
    public static class RiskInfo {

        private String riskId;

        private String queueId;

        private String unitId;
        private String unitStatus;

        private String districtId;
        private String districtName;

        private String stepId;

        private Double riskScore;

        private Double estimatedDelayHr;

        private Double delayProbability;

        private String riskLevel;

        private String riskFactor;

        private LocalDateTime detectionTime;

        public static RiskInfo from(DelayRisk risk) {

            return RiskInfo.builder()
                    .riskId(risk.getRiskId())

                    .queueId(risk.getQueueId())

                    .unitId(
                            risk.getUnit() != null
                                    ? risk.getUnit().getUnitId()
                                    : null
                    )
                    .unitStatus(
                            risk.getUnit() != null
                                    ? risk.getUnit().getUnitStatus()
                                    : null
                    )

                    .districtId(
                            risk.getDistrict() != null
                                    ? risk.getDistrict().getDistrictId()
                                    : null
                    )
                    .districtName(
                            risk.getDistrict() != null
                                    ? risk.getDistrict().getDistrictName()
                                    : null
                    )

                    .stepId(risk.getStepId())

                    .riskScore(risk.getRiskScore())

                    .estimatedDelayHr(risk.getEstimatedDelayHr())

                    .delayProbability(risk.getDelayProbability())

                    .riskLevel(risk.getRiskLevel())

                    .riskFactor(risk.getRiskFactor())

                    .detectionTime(risk.getDetectionTime())

                    .build();
        }
    }
}