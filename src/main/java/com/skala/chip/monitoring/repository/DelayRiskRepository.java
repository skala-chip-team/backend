package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.DelayRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DelayRiskRepository extends JpaRepository<DelayRisk, String> {

    // 현재 대기열(process_queue)에 unit 이 남아있는 High/Critical 위험 수 (= 실제 감지 대상).
    // 시뮬 자동 realtime 전환 트리거 판정에 사용.
    @Query("select count(d) from DelayRisk d "
            + "where upper(d.riskLevel) in ('HIGH','CRITICAL') "
            + "and exists (select 1 from ProcessQueue q "
            + "            where q.unit.unitId = d.unit.unitId and q.stepId = d.stepId)")
    long countActionableHighCritical();

    List<DelayRisk> findByRiskLevelIn(List<String> riskLevels);

    List<DelayRisk> findByUnit_UnitId(String unitId);

    List<DelayRisk> findByDistrict_DistrictId(String districtId);

    // (구역, step) 의 현재 위험 목록. 재조정안 생성 시 stale risk_id 자가복구에 사용한다.
    List<DelayRisk> findByDistrict_DistrictIdAndStepId(String districtId, String stepId);

    // risk_id 목록으로 위험 조회 (재조정 그룹 상세용)
    List<DelayRisk> findByRiskIdIn(List<String> riskIds);

    // 모델 실행 배치(시 단위) 조회: [start, end) 범위의 위험 목록
    List<DelayRisk> findByDetectionTimeGreaterThanEqualAndDetectionTimeLessThan(
            LocalDateTime start, LocalDateTime end);

    // 가장 최신 배치 시각을 찾기 위한 조회
    Optional<DelayRisk> findTopByOrderByDetectionTimeDesc();
}