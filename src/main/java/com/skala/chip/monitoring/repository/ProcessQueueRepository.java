package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.ProcessQueue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessQueueRepository extends JpaRepository<ProcessQueue, String> {

    List<ProcessQueue> findByStepIdOrderByQueuePositionAsc(String stepId);

    List<ProcessQueue> findByDistrict_DistrictIdOrderByQueuePositionAsc(String districtId);

    List<ProcessQueue> findByUnit_UnitId(String unitId);
}