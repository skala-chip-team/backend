package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.MachineStatusEvent;
import com.skala.chip.monitoring.domain.MachineStepMap;
import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.domain.WorkStatus;
import com.skala.chip.monitoring.dto.DistrictMachineResponseDTO;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.MachineStatusEventRepository;
import com.skala.chip.monitoring.repository.MachineStepMapRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.repository.WorkStatusRepository;
import com.skala.chip.monitoring.service.DistrictMachineService;
import com.skala.chip.monitoring.service.SimClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DistrictMachineServiceImpl implements DistrictMachineService {

    private final DistrictRepository districtRepository;
    private final MachineStepMapRepository machineStepMapRepository;
    private final MachineStatusEventRepository machineStatusEventRepository;
    private final ScheduleRepository scheduleRepository;
    private final WorkStatusRepository workStatusRepository;
    private final SimClock simClock;

    @Override
    @Transactional(readOnly = true)
    public DistrictMachineResponseDTO.DistrictMachines getDistrictMachines(String districtId, String stepId) {
        var district = districtRepository.findById(districtId)
                .orElseThrow(() -> new IllegalArgumentException("구역을 찾을 수 없습니다. districtId=" + districtId));

        List<MachineStepMap> stepMaps = (stepId != null && !stepId.isBlank())
                ? machineStepMapRepository.findByMachine_District_DistrictIdAndStep_StepId(districtId, stepId)
                : machineStepMapRepository.findByMachine_District_DistrictId(districtId);

        // 시뮬레이션 데이터는 sim 달력 기준이라 실제 LocalDate.now() 가 아닌 sim 기준 "오늘" 을 쓴다.
        LocalDateTime todayStart = simClock.now().toLocalDate().atStartOfDay();

        List<DistrictMachineResponseDTO.MachineDetail> machines = stepMaps.stream()
                .map(map -> buildMachineDetail(map, todayStart))
                .toList();

        return DistrictMachineResponseDTO.DistrictMachines.builder()
                .districtId(district.getDistrictId())
                .districtName(district.getDistrictName())
                .machines(machines)
                .build();
    }

    private DistrictMachineResponseDTO.MachineDetail buildMachineDetail(MachineStepMap map, LocalDateTime todayStart) {
        var machine = map.getMachine();
        var step = map.getStep();

        double utilizationRate = calculateUtilizationRate(machine.getMachineId(), todayStart);

        DistrictMachineResponseDTO.ActiveSchedule activeSchedule = scheduleRepository
                .findByMachine_MachineIdAndStatusAndActiveTrue(machine.getMachineId(), "진행중")
                .stream()
                .findFirst()
                .map(schedule -> buildActiveSchedule(schedule, step.getStepAvgTime()))
                .orElse(null);

        return DistrictMachineResponseDTO.MachineDetail.builder()
                .machineId(machine.getMachineId())
                .machineType(machine.getMachineType())
                .machineStatus(machine.getMachineStatus())
                .stepId(step.getStepId())
                .processStep(step.getProcessStep())
                .utilizationRate(utilizationRate)
                .activeSchedule(activeSchedule)
                .build();
    }

    private DistrictMachineResponseDTO.ActiveSchedule buildActiveSchedule(ScheduleMaster schedule, Integer stepAvgTime) {
        LocalDateTime startTime = workStatusRepository
                .findBySchedule_ScheduleId(schedule.getScheduleId())
                .stream()
                .map(WorkStatus::getStartTime)
                .filter(t -> t != null)
                .findFirst()
                .orElse(null);

        LocalDateTime estimatedEnd = (startTime != null && stepAvgTime != null)
                ? startTime.plusMinutes(stepAvgTime)
                : null;

        return DistrictMachineResponseDTO.ActiveSchedule.builder()
                .scheduleId(schedule.getScheduleId())
                .unitId(schedule.getUnit() != null ? schedule.getUnit().getUnitId() : null)
                .startTime(startTime)
                .estimatedEnd(estimatedEnd)
                .priority(schedule.getPriority())
                .build();
    }

    private double calculateUtilizationRate(String machineId, LocalDateTime todayStart) {
        List<MachineStatusEvent> events = machineStatusEventRepository
                .findByMachineIdAndStartTimeGreaterThanEqual(machineId, todayStart);

        long runningSeconds = events.stream()
                .filter(e -> "가동".equals(e.getStatus()) && e.getEndTime() != null)
                .mapToLong(e -> Duration.between(e.getStartTime(), e.getEndTime()).getSeconds())
                .sum();

        LocalDateTime maxEndTime = events.stream()
                .filter(e -> e.getEndTime() != null)
                .map(MachineStatusEvent::getEndTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        if (maxEndTime == null || !maxEndTime.isAfter(todayStart)) {
            return 0.0;
        }

        long elapsedSeconds = Duration.between(todayStart, maxEndTime).getSeconds();
        double rate = (double) runningSeconds / elapsedSeconds * 100;
        return Math.round(rate * 10.0) / 10.0;
    }
}
