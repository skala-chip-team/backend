package com.skala.chip.order.domain;

import com.skala.chip.monitoring.domain.DistrictMaster;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주문 마스터(읽기 전용). 테이블은 외부(AI 시뮬레이션)가 소유하므로 매핑만 한다.
 *
 * due_date 는 DB 상 timestamp(시각 포함) 이므로 LocalDateTime 으로 매핑한다.
 */
@Entity
@Table(name = "TT_DAILY_ORDER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyOrder {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id")
    private DistrictMaster district;

    @Column(name = "plan_date")
    private LocalDate planDate;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "planned_output_qty")
    private Integer plannedOutputQty;

    // 1(매우 높음) ~ 5(매우 낮음)
    @Column(name = "order_priority")
    private Integer orderPriority;

    // burst 시나리오로 추가된 긴급 주문 여부 (프론트 긴급 뱃지)
    @Column(name = "is_burst")
    private Boolean isBurst;
}
