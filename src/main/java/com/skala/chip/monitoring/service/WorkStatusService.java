package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.WorkStatusResponseDTO;

import java.util.List;

public interface WorkStatusService {

    List<WorkStatusResponseDTO.WorkStatusInfo> getWorkStatuses();

    List<WorkStatusResponseDTO.WorkStatusInfo> getActiveWorkStatuses();

    List<WorkStatusResponseDTO.WorkStatusInfo> getMachineWorkStatuses(String machineId);

    List<WorkStatusResponseDTO.WorkStatusInfo> getDistrictWorkStatuses(String districtId);
}