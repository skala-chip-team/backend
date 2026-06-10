package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TT_WORK_STATUS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkStatus {

    @Id
    @Column(name = "status_id")
    private String statusId;

    // LAZY: work_status 목록 조회 시 행마다 schedule_master/machine_master 를 추가 조회하는
    // N+1 폭발을 막는다. 실제 접근하는 WorkStatusServiceImpl 은 모두 @Transactional 이라 안전.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private ScheduleMaster schedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id")
    private MachineMaster machine;

    @Column(name = "operator_id")
    private String operatorId;

    @Column(name = "shift")
    private String shift;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "defect_count")
    private Integer defectCount;

    @Column(name = "output_qty")
    private Integer outputQty;
}