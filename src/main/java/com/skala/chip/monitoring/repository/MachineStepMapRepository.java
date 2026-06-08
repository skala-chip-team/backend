package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.MachineStepMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineStepMapRepository extends JpaRepository<MachineStepMap, String> {

    List<MachineStepMap> findByMachine_District_DistrictId(String districtId);

    List<MachineStepMap> findByMachine_District_DistrictIdAndStep_StepId(String districtId, String stepId);
}
