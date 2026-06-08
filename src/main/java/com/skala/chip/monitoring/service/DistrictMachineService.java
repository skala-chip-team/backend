package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.DistrictMachineResponseDTO;

public interface DistrictMachineService {

    DistrictMachineResponseDTO.DistrictMachines getDistrictMachines(String districtId, String stepId);
}
