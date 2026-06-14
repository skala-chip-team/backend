package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.ProcessQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessQueueRepository extends JpaRepository<ProcessQueue, String> {

    List<ProcessQueue> findByStepIdOrderByQueuePositionAsc(String stepId);

    List<ProcessQueue> findByDistrict_DistrictIdOrderByQueuePositionAsc(String districtId);

    List<ProcessQueue> findByUnit_UnitId(String unitId);

    // (구역, step) 의 현재 대기열. 재조정 가능(actionable) 위험 판별에 사용한다.
    List<ProcessQueue> findByDistrict_DistrictIdAndStepId(String districtId, String stepId);
}