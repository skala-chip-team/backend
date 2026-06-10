package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TT_PROCESS_QUEUE")
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

    // 실제 process_queue 테이블에 없는 컬럼들(AI sim 스키마 기준) → 조회 SELECT 에서 제외.
    @Transient
    private String optionId;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "enqueue_time")
    private LocalDateTime enqueueTime;

    @Transient
    private Double actualWaitTime;

    @Transient
    private String status;
}