package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TM_SCHEDULE_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleMaster {

    @Id
    @Column(name = "schedule_id")
    private String scheduleId;

    @Column(name = "queue_id")
    private String queueId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unit_id")
    private UnitMaster unit;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "machine_id")
    private MachineMaster machine;

    @Column(name = "step_id")
    private String stepId;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "estimated_start")
    private LocalDateTime estimatedStart;

    @Column(name = "status")
    private String status;

    @Column(name = "active")
    private Boolean active;
}