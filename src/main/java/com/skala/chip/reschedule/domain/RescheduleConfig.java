package com.skala.chip.reschedule.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 재조정 관련 설정 값 저장 (key-value).
 * 현재는 위험 그룹핑 임계값(RISK_THRESHOLD)을 사용자가 조정할 수 있도록 보관한다.
 */
@Entity
@Table(name = "reschedule_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleConfig {

    @Id
    @Column(name = "config_key")
    private String configKey;

    @Column(name = "config_value")
    private String configValue;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
