package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "process_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessQueue {

    @Id
    @Column(name = "queue_id")
    private String queueId;

    @Column(name = "step_id")
    private String stepId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id")
    private DistrictMaster district;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "unit_id")
    private UnitMaster unit;

    @Column(name = "option_id")
    private String optionId;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "enqueue_time")
    private LocalDateTime enqueueTime;

    @Column(name = "actual_wait_time")
    private Double actualWaitTime;

    @Column(name = "status")
    private String status;
}