package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkStatus {

    @Id
    @Column(name = "status_id")
    private String statusId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "schedule_id")
    private ScheduleMaster schedule;

    @ManyToOne(fetch = FetchType.EAGER)
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