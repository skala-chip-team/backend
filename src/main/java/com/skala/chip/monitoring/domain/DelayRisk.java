package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TT_DELAY_RISK")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DelayRisk {

    @Id
    @Column(name = "risk_id")
    private String riskId;

    @Column(name = "queue_id")
    private String queueId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unit_id")
    private UnitMaster unit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id")
    private DistrictMaster district;

    @Column(name = "step_id")
    private String stepId;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "estimated_delay_hr")
    private Double estimatedDelayHr;

    @Column(name = "delay_probability")
    private Double delayProbability;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "risk_factor")
    private String riskFactor;

    @Column(name = "detection_time")
    private LocalDateTime detectionTime;
}