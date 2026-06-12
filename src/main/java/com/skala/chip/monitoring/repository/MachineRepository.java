package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.MachineMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineRepository extends JpaRepository<MachineMaster, String> {

    long countByMachineStatus(String machineStatus);

    long countByDistrict_DistrictId(String districtId);

    long countByDistrict_DistrictIdAndMachineStatus(String districtId, String machineStatus);

    // 구역별 전체 장비 목록 (overview: 기계 1대 = 1행 보장용)
    List<MachineMaster> findByDistrict_DistrictId(String districtId);
}