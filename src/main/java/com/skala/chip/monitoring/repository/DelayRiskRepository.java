package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.DelayRisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DelayRiskRepository extends JpaRepository<DelayRisk, String> {

    List<DelayRisk> findByRiskLevelIn(List<String> riskLevels);

    List<DelayRisk> findByUnit_UnitId(String unitId);

    List<DelayRisk> findByDistrict_DistrictId(String districtId);

    // risk_id 목록으로 위험 조회 (재조정 그룹 상세용)
    List<DelayRisk> findByRiskIdIn(List<String> riskIds);

    // 모델 실행 배치(시 단위) 조회: [start, end) 범위의 위험 목록
    List<DelayRisk> findByDetectionTimeGreaterThanEqualAndDetectionTimeLessThan(
            LocalDateTime start, LocalDateTime end);

    // 가장 최신 배치 시각을 찾기 위한 조회
    Optional<DelayRisk> findTopByOrderByDetectionTimeDesc();
}