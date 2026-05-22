package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.dto.ScheduleResponseDTO;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO.ScheduleInfo> getSchedules() {

        List<ScheduleMaster> schedules = scheduleRepository.findAll();

        return schedules.stream()
                .map(ScheduleResponseDTO.ScheduleInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO.ScheduleInfo> getActiveSchedules() {

        List<ScheduleMaster> schedules = scheduleRepository.findByActiveTrue();

        return schedules.stream()
                .map(ScheduleResponseDTO.ScheduleInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO.ScheduleInfo> getMachineSchedules(String machineId) {

        List<ScheduleMaster> schedules =
                scheduleRepository.findByMachine_MachineId(machineId);

        return schedules.stream()
                .map(ScheduleResponseDTO.ScheduleInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponseDTO.ScheduleInfo> getUnitSchedules(String unitId) {

        List<ScheduleMaster> schedules =
                scheduleRepository.findByUnit_UnitId(unitId);

        return schedules.stream()
                .map(ScheduleResponseDTO.ScheduleInfo::from)
                .toList();
    }
}