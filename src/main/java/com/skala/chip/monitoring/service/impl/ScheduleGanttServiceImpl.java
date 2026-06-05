package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.dto.ScheduleGanttResponseDTO;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.service.ScheduleGanttService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleGanttServiceImpl implements ScheduleGanttService {

    private final DistrictRepository districtRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public ScheduleGanttResponseDTO.DistrictGantt getDistrictGantt(String districtId) {

        DistrictMaster district = districtRepository.findById(districtId).orElse(null);

        // 공정 단계 정의(step_avg_time 등)를 stepId 기준 Map 으로 적재
        Map<String, ProcessStepOrder> stepMap = processStepOrderRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ProcessStepOrder::getStepId,
                        Function.identity(),
                        (a, b) -> a
                ));

        // 해당 구역 장비에 배정된 스케줄을 stepId 기준으로 그룹핑
        Map<String, List<ScheduleMaster>> schedulesByStep =
                scheduleRepository.findByMachine_District_DistrictId(districtId).stream()
                        .filter(s -> s.getStepId() != null)
                        .collect(Collectors.groupingBy(ScheduleMaster::getStepId));

        List<ScheduleGanttResponseDTO.StepGantt> steps = schedulesByStep.entrySet().stream()
                .map(entry -> {
                    String stepId = entry.getKey();
                    ProcessStepOrder stepDef = stepMap.get(stepId);
                    Integer avgTime = stepDef != null ? stepDef.getStepAvgTime() : null;

                    List<ScheduleGanttResponseDTO.GanttBar> bars = entry.getValue().stream()
                            .map(s -> toBar(s, avgTime))
                            .sorted(Comparator.comparing(
                                    ScheduleGanttResponseDTO.GanttBar::getEstimatedStart,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .toList();

                    return ScheduleGanttResponseDTO.StepGantt.builder()
                            .stepId(stepId)
                            .processStep(stepDef != null ? stepDef.getProcessStep() : null)
                            .stepOrder(stepDef != null ? stepDef.getStepOrder() : null)
                            .stepAvgTime(avgTime)
                            .schedules(bars)
                            .build();
                })
                // 공정 순서(step_order) 오름차순 정렬, 정의 없는 step 은 뒤로
                .sorted(Comparator.comparing(
                        ScheduleGanttResponseDTO.StepGantt::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return ScheduleGanttResponseDTO.DistrictGantt.builder()
                .districtId(districtId)
                .districtName(district != null ? district.getDistrictName() : null)
                .steps(steps)
                .build();
    }

    /**
     * 스케줄 1건을 간트 막대로 변환한다.
     * estimatedEnd = estimatedStart + step_avg_time(분).
     * 시작 시각이나 평균 소요시간이 없으면 종료 시각은 null 로 둔다.
     */
    private ScheduleGanttResponseDTO.GanttBar toBar(ScheduleMaster schedule, Integer stepAvgTime) {

        LocalDateTime start = schedule.getEstimatedStart();
        LocalDateTime end = (start != null && stepAvgTime != null)
                ? start.plusMinutes(stepAvgTime)
                : null;

        return ScheduleGanttResponseDTO.GanttBar.builder()
                .scheduleId(schedule.getScheduleId())
                .machineId(
                        schedule.getMachine() != null
                                ? schedule.getMachine().getMachineId()
                                : null
                )
                .machineStatus(
                        schedule.getMachine() != null
                                ? schedule.getMachine().getMachineStatus()
                                : null
                )
                .unitId(
                        schedule.getUnit() != null
                                ? schedule.getUnit().getUnitId()
                                : null
                )
                .unitStatus(
                        schedule.getUnit() != null
                                ? schedule.getUnit().getUnitStatus()
                                : null
                )
                .priority(schedule.getPriority())
                .status(schedule.getStatus())
                .active(schedule.getActive())
                .estimatedStart(start)
                .estimatedEnd(end)
                .build();
    }
}
