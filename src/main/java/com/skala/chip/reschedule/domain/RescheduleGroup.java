package com.skala.chip.reschedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 같은 step queue(구역+공정) 기준으로 묶인 재조정 검토 그룹.
 * delay_risk 원본은 수정하지 않고, 같은 step 의 위험 예측들을 묶어 운영자가 검토하는 단위.
 */
@Entity
@Table(name = "reschedule_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleGroup {

    @Id
    @Column(name = "group_id")
    private String groupId;

    @Column(name = "district_id", nullable = false)
    private String districtId;

    @Column(name = "step_id", nullable = false)
    private String stepId;

    // 이 그룹에 포함된 delay_risk.risk_id 목록
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "member_risk_ids", columnDefinition = "jsonb", nullable = false)
    private List<String> memberRiskIds;

    // 그룹 내 delay_risk.delay_probability 중 최대값
    @Column(name = "max_risk_score")
    private Double maxRiskScore;

    // pending / approved / expired
    @Column(name = "group_status")
    private String groupStatus;

    // 재조정안 상세 내용 (에이전트 호출 결과가 채워짐, 초기 null)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reschedule_detail", columnDefinition = "jsonb")
    private Map<String, Object> rescheduleDetail;

    // PoC 통합 시각
    @Column(name = "acted_at")
    private LocalDateTime actedAt;
}
