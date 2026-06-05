package com.skala.chip.reschedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 재조정 그룹에서 운영자가 선택·확정한 전략(재조정 스케줄).
 * "어떤 재조정이 선택되었는지" 를 쉽게 조회하기 위한 테이블. 그룹당 1건만 확정한다.
 */
@Entity
@Table(name = "reschedule_selection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleSelection {

    @Id
    @Column(name = "selection_id")
    private String selectionId;

    // 그룹당 1건만 확정되므로 group_id 는 유일하다
    @Column(name = "group_id", nullable = false, unique = true)
    private String groupId;

    @Column(name = "strategy", nullable = false)
    private String strategy;

    // 선택된 전략 상세 통째 (queue_reorder, after_schedule, dispatch_simulation 등)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_detail", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> selectedDetail;

    // applied / reverted
    @Column(name = "status")
    private String status;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;
}
