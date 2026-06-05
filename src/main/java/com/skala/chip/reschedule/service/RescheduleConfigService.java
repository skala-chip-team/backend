package com.skala.chip.reschedule.service;

import com.skala.chip.reschedule.domain.RescheduleConfig;
import com.skala.chip.reschedule.repository.RescheduleConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 재조정 설정 값 관리. 위험 그룹핑 임계값(risk threshold)을 DB 에 저장/조회한다.
 * 값이 저장되어 있지 않으면 기본값 0.7 을 사용한다.
 */
@Service
@RequiredArgsConstructor
public class RescheduleConfigService {

    private static final String KEY_RISK_THRESHOLD = "RISK_THRESHOLD";
    private static final double DEFAULT_RISK_THRESHOLD = 0.7;

    private final RescheduleConfigRepository configRepository;

    @Transactional(readOnly = true)
    public double getRiskThreshold() {
        return configRepository.findById(KEY_RISK_THRESHOLD)
                .map(c -> parseOrDefault(c.getConfigValue()))
                .orElse(DEFAULT_RISK_THRESHOLD);
    }

    @Transactional
    public double updateRiskThreshold(double value) {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(
                    "threshold 는 0.0 ~ 1.0 사이여야 합니다: " + value
            );
        }

        RescheduleConfig config = configRepository.findById(KEY_RISK_THRESHOLD)
                .orElseGet(() -> RescheduleConfig.builder()
                        .configKey(KEY_RISK_THRESHOLD)
                        .build());

        config.setConfigValue(String.valueOf(value));
        config.setUpdatedAt(LocalDateTime.now());
        configRepository.save(config);

        return value;
    }

    private double parseOrDefault(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException | NullPointerException e) {
            return DEFAULT_RISK_THRESHOLD;
        }
    }
}
