package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.DistrictMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface DistrictRepository extends JpaRepository<DistrictMaster, String> {

    /**
     * 시뮬레이션 기준 현재 시각 = {@code TS_DISTRICT_STATUS} 의 최신 스냅샷 시각.
     * 해당 테이블은 백엔드 엔티티가 없고 시뮬레이션 파이프라인이 생성하므로 네이티브 쿼리로 조회한다.
     * 데이터가 없으면 {@code null} 을 반환한다.
     */
    @Query(value = "SELECT MAX(created_at) FROM TS_DISTRICT_STATUS", nativeQuery = true)
    LocalDateTime findLatestSimTime();
}