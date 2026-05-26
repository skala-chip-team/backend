package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.WorkStatus;
import com.skala.chip.monitoring.dto.WorkStatusResponseDTO;
import com.skala.chip.monitoring.repository.WorkStatusRepository;
import com.skala.chip.monitoring.service.WorkStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkStatusServiceImpl implements WorkStatusService {

    private final WorkStatusRepository workStatusRepository;

    @Override
    @Transactional(readOnly = true)
    public List<WorkStatusResponseDTO.WorkStatusInfo> getWorkStatuses() {
        return workStatusRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkStatusResponseDTO.WorkStatusInfo> getActiveWorkStatuses() {
        return workStatusRepository.findByEndTimeIsNull()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkStatusResponseDTO.WorkStatusInfo> getMachineWorkStatuses(String machineId) {
        return workStatusRepository.findByMachine_MachineId(machineId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkStatusResponseDTO.WorkStatusInfo> getDistrictWorkStatuses(String districtId) {
        return workStatusRepository.findByMachine_District_DistrictId(districtId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private WorkStatusResponseDTO.WorkStatusInfo toDto(WorkStatus workStatus) {

        return WorkStatusResponseDTO.WorkStatusInfo.builder()
                .statusId(workStatus.getStatusId())
                .scheduleId(workStatus.getSchedule().getScheduleId())
                .machineId(workStatus.getMachine().getMachineId())
                .machineStatus(workStatus.getMachine().getMachineStatus())
                .districtId(workStatus.getMachine().getDistrict().getDistrictId())
                .unitId(workStatus.getSchedule().getUnit().getUnitId())
                .operatorId(workStatus.getOperatorId())
                .shift(workStatus.getShift())
                .startTime(workStatus.getStartTime())
                .endTime(workStatus.getEndTime())
                .defectCount(workStatus.getDefectCount())
                .outputQty(workStatus.getOutputQty())
                .build();
    }
}