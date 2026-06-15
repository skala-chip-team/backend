package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.MachineAdminDTO;

import java.util.List;

/** 장비 설정(ADMIN) — 장비 CRUD + 공정 STEP 옵션. */
public interface MachineAdminService {

    List<MachineAdminDTO.MachineItem> getMachines(String districtId, String stepId);

    List<MachineAdminDTO.ProcessStepOption> getProcessSteps();

    MachineAdminDTO.MachineItem createMachine(MachineAdminDTO.UpsertRequest request);

    MachineAdminDTO.MachineItem updateMachine(String machineId, MachineAdminDTO.UpsertRequest request);

    void deleteMachine(String machineId);
}
