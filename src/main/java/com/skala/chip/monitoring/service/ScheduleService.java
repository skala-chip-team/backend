package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.ScheduleResponseDTO;

import java.util.List;

public interface ScheduleService {

    List<ScheduleResponseDTO.ScheduleInfo> getSchedules();

    List<ScheduleResponseDTO.ScheduleInfo> getActiveSchedules();

    List<ScheduleResponseDTO.ScheduleInfo> getMachineSchedules(String machineId);

    List<ScheduleResponseDTO.ScheduleInfo> getUnitSchedules(String unitId);
}