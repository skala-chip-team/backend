package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.MachineMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MachineRepository extends JpaRepository<MachineMaster, String> {

    long countByMachineStatus(String machineStatus);

    long countByDistrict_DistrictId(String districtId);

    long countByDistrict_DistrictIdAndMachineStatus(String districtId, String machineStatus);
}