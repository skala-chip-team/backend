package com.skala.chip.monitoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "process_step_order")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessStepOrder {

    @Id
    @Column(name = "step_id")
    private String stepId;

    @Column(name = "process_step")
    private String processStep;

    @Column(name = "step_order")
    private Integer stepOrder;

    // 공정 단계 평균 소요시간 (분)
    @Column(name = "step_avg_time")
    private Integer stepAvgTime;
}
