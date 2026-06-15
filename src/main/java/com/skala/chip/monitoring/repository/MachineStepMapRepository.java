package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.MachineStepMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineStepMapRepository extends JpaRepository<MachineStepMap, String> {

    List<MachineStepMap> findByMachine_District_DistrictId(String districtId);

    List<MachineStepMap> findByMachine_District_DistrictIdAndStep_StepId(String districtId, String stepId);

    // 특정 장비의 공정 매핑 (장비 설정 조회/수정/삭제용). 보통 1건.
    List<MachineStepMap> findByMachine_MachineId(String machineId);
}
