package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.DelayRisk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelayRiskRepository extends JpaRepository<DelayRisk, String> {

    List<DelayRisk> findByRiskLevelIn(List<String> riskLevels);

    List<DelayRisk> findByUnit_UnitId(String unitId);

    List<DelayRisk> findByDistrict_DistrictId(String districtId);
}