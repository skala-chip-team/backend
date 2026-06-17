package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.dto.ScheduleGanttResponseDTO;
import com.skala.chip.monitoring.domain.WorkStatus;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.repository.WorkStatusRepository;
import com.skala.chip.monitoring.service.ScheduleGanttService;
import com.skala.chip.monitoring.service.SimClock;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import com.skala.chip.reschedule.repository.RescheduleSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleGanttServiceImpl implements ScheduleGanttService {

    private final DistrictRepository districtRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final WorkStatusRepository workStatusRepository;
    private final SimClock simClock;
    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final RescheduleSelectionRepository rescheduleSelectionRepository;

    private static final String GROUP_STATUS_APPROVED = "approved";

    @Override
    @Transactional(readOnly = true)
    public ScheduleGanttResponseDTO.DistrictGantt getDistrictGantt(String districtId) {

        DistrictMaster district = districtRepository.findById(districtId).orElse(null);

        // 승인된 재조정으로 변경된 (unit|step) 키 집합 — 간트 막대 "재조정 반영됨" 표시용
        Set<String> rescheduledKeys = buildRescheduledKeys(districtId);

        // 실제 작업 상태(work_status)를 scheduleId 기준으로 적재. 한 스케줄에 여러 건이면 가장 최근 시작을 사용.
        Map<String, WorkStatus> workBySchedule = workStatusRepository
                .findByMachine_District_DistrictId(districtId).stream()
                .filter(w -> w.getSchedule() != null && w.getSchedule().getScheduleId() != null)
                .collect(Collectors.toMap(
                        w -> w.getSchedule().getScheduleId(),
                        Function.identity(),
                        (a, b) -> {
                            LocalDateTime sa = a.getStartTime();
                            LocalDateTime sb = b.getStartTime();
                            if (sa == null) return b;
                            if (sb == null) return a;
                            return sb.isAfter(sa) ? b : a;
                        }
                ));

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
                            .map(s -> toBar(s, avgTime, workBySchedule.get(s.getScheduleId()), rescheduledKeys))
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
                .simulatedAt(simClock.now())
                .steps(steps)
                .build();
    }

    /**
     * 스케줄 1건을 간트 막대로 변환한다.
     * - estimatedStart/estimatedEnd : 계획(예측) 구간. estimatedEnd = estimatedStart + step_avg_time(분).
     * - work_status 가 있으면 actualStart/actualEnd(실제 시작/종료)를 함께 채운다.
     * - 진행 중(실제 시작 O, 실제 종료 X)이면 projectedEnd = actualStart + step_avg_time 로 예상 종료를 산출한다.
     */
    private ScheduleGanttResponseDTO.GanttBar toBar(ScheduleMaster schedule, Integer stepAvgTime,
                                                    WorkStatus work, Set<String> rescheduledKeys) {

        LocalDateTime start = schedule.getEstimatedStart();
        boolean rescheduled = schedule.getUnit() != null
                && rescheduledKeys.contains(schedule.getUnit().getUnitId() + "|" + schedule.getStepId());
        LocalDateTime end = (start != null && stepAvgTime != null)
                ? start.plusMinutes(stepAvgTime)
                : null;

        LocalDateTime actualStart = work != null ? work.getStartTime() : null;
        LocalDateTime actualEnd = work != null ? work.getEndTime() : null;
        // 진행 중(실제 시작 O, 실제 종료 X): 평균 소요시간으로 예상 종료 산출
        LocalDateTime projectedEnd = (actualStart != null && actualEnd == null && stepAvgTime != null)
                ? actualStart.plusMinutes(stepAvgTime)
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
                .actualStart(actualStart)
                .actualEnd(actualEnd)
                .projectedEnd(projectedEnd)
                .rescheduled(rescheduled)
                .build();
    }

    /**
     * 해당 구역에서 승인(approved)된 재조정의 영향 (unit|step) 키 집합을 구성한다.
     * reschedule_selection.selected_detail.after_schedule.units[].steps[].step_id 를 역산한다.
     */
    @SuppressWarnings("unchecked")
    private Set<String> buildRescheduledKeys(String districtId) {
        Set<String> keys = new HashSet<>();
        for (RescheduleGroup group : rescheduleGroupRepository.findByDistrictId(districtId)) {
            if (!GROUP_STATUS_APPROVED.equals(group.getGroupStatus())) {
                continue;
            }
            rescheduleSelectionRepository.findByGroupId(group.getGroupId()).ifPresent(sel -> {
                Object detail = sel.getSelectedDetail();
                if (!(detail instanceof Map<?, ?> opt) || !(opt.get("after_schedule") instanceof Map<?, ?> sch)
                        || !(sch.get("units") instanceof List<?> units)) {
                    return;
                }
                for (Object u : units) {
                    if (!(u instanceof Map<?, ?> unit) || !(unit.get("unit_id") instanceof String uid)
                            || !(unit.get("steps") instanceof List<?> stepList)) {
                        continue;
                    }
                    for (Object st : stepList) {
                        if (st instanceof Map<?, ?> step && step.get("step_id") instanceof String sid) {
                            keys.add(uid + "|" + sid);
                        }
                    }
                }
            });
        }
        return keys;
    }
}
