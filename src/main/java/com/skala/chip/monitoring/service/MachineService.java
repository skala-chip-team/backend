package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.MachineResponseDTO;

import java.util.List;

public interface MachineService {

    List<MachineResponseDTO.MachineInfo> getMachines();

    MachineResponseDTO.MachineInfo getMachine(String machineId);

    MachineResponseDTO.MachineInfo updateMachineStatus(String machineId, String machineStatus);
}