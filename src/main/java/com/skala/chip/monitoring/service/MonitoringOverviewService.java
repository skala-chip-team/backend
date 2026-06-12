package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.MachineMaster;
import com.skala.chip.monitoring.domain.MachineStatusEvent;
import com.skala.chip.monitoring.domain.MachineStepMap;
import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.domain.WorkStatus;
import com.skala.chip.monitoring.dto.MonitoringOverviewResponseDTO;
import com.skala.chip.monitoring.dto.MonitoringOverviewResponseDTO.DistrictOverview;
import com.skala.chip.monitoring.dto.MonitoringOverviewResponseDTO.LatestReschedule;
import com.skala.chip.monitoring.dto.MonitoringOverviewResponseDTO.RiskItem;
import com.skala.chip.monitoring.dto.MonitoringOverviewResponseDTO.StepQueue;
import com.skala.chip.monitoring.dto.MonitoringOverviewResponseDTO.Summary;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.repository.MachineStatusEventRepository;
import com.skala.chip.monitoring.repository.MachineStepMapRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.repository.WorkStatusRepository;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 전체 대시보드(overview) 조합 서비스.
 * 모든 구역의 summary/machines/stepQueues/reschedule 를 한 트랜잭션·한 시뮬 스냅샷으로 묶어 반환한다.
 */
@Service
@RequiredArgsConstructor
public class MonitoringOverviewService {

    private static final String STATUS_RUNNING = "가동";
    private static final String STATUS_MAINTENANCE = "점검중";
    private static final String STATUS_DOWN = "정지";

    private static final String GROUP_STATUS_PENDING = "pending";
    private static final String GROUP_STATUS_APPROVED = "approved";

    private final DistrictRepository districtRepository;
    private final MachineRepository machineRepository;
    private final MachineStepMapRepository machineStepMapRepository;
    private final MachineStatusEventRepository machineStatusEventRepository;
    private final ScheduleRepository scheduleRepository;
    private final ProcessQueueRepository processQueueRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final WorkStatusRepository workStatusRepository;
    private final DelayRiskRepository delayRiskRepository;
    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final SimClock simClock;

    @Transactional(readOnly = true)
    public List<DistrictOverview> getOverview() {
        // 모든 구역을 같은 시점으로 보기 위해 스냅샷 시각을 한 번만 캡처
        LocalDateTime simNow = simClock.now();
        LocalDate today = simNow.toLocalDate();
        LocalDateTime todayStart = today.atStartOfDay();

        // 공정 단계 정의 (stepId -> process_step, step_order)
        Map<String, ProcessStepOrder> stepMap = processStepOrderRepository.findAll().stream()
                .collect(Collectors.toMap(ProcessStepOrder::getStepId, Function.identity(), (a, b) -> a));

        return districtRepository.findAll().stream()
                .sorted(Comparator.comparing(DistrictMaster::getDistrictId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(d -> buildDistrict(d, stepMap, today, todayStart))
                .toList();
    }

    private DistrictOverview buildDistrict(DistrictMaster district,
                                           Map<String, ProcessStepOrder> stepMap,
                                           LocalDate today, LocalDateTime todayStart) {
        String districtId = district.getDistrictId();

        // machine_id -> 대표 step (step_order 최소). 한 기계가 여러 step 에 매핑돼도 1행으로 유지.
        Map<String, ProcessStepOrder> stepByMachine = machineStepMapRepository
                .findByMachine_District_DistrictId(districtId).stream()
                .filter(m -> m.getMachine() != null && m.getStep() != null)
                .collect(Collectors.toMap(
                        m -> m.getMachine().getMachineId(),
                        MachineStepMap::getStep,
                        (a, b) -> stepOrderOf(a) <= stepOrderOf(b) ? a : b));

        // 구역 스케줄: 활성 작업(machine -> unit), 그리고 (unit,step) -> machine 매핑(위험 machineId 유추용)
        List<ScheduleMaster> schedules = scheduleRepository.findByMachine_District_DistrictId(districtId);
        Map<String, String> activeUnitByMachine = schedules.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()) && "진행중".equals(s.getStatus()))
                .filter(s -> s.getMachine() != null && s.getUnit() != null)
                .collect(Collectors.toMap(
                        s -> s.getMachine().getMachineId(),
                        s -> s.getUnit().getUnitId(),
                        (a, b) -> a));
        Map<String, String> machineByUnitStep = schedules.stream()
                .filter(s -> s.getMachine() != null && s.getUnit() != null && s.getStepId() != null)
                .collect(Collectors.toMap(
                        s -> s.getUnit().getUnitId() + "|" + s.getStepId(),
                        s -> s.getMachine().getMachineId(),
                        (a, b) -> a));

        // 1) machines — machine_master 기준 1기계=1행
        List<MachineMaster> machineRows = machineRepository.findByDistrict_DistrictId(districtId);
        List<MonitoringOverviewResponseDTO.Machine> machines = machineRows.stream()
                .sorted(Comparator.comparing(MachineMaster::getMachineId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(m -> {
                    ProcessStepOrder step = stepByMachine.get(m.getMachineId());
                    boolean down = STATUS_DOWN.equals(m.getMachineStatus());
                    LocalDateTime faultSince = down
                            ? machineStatusEventRepository
                                .findTopByMachineIdAndStatusOrderByStartTimeDesc(m.getMachineId(), STATUS_DOWN)
                                .map(MachineStatusEvent::getStartTime).orElse(null)
                            : null;
                    return MonitoringOverviewResponseDTO.Machine.builder()
                            .machineId(m.getMachineId())
                            .machineStatus(m.getMachineStatus())
                            .processStep(step != null ? step.getProcessStep() : null)
                            .stepOrder(step != null ? step.getStepOrder() : null)
                            .utilizationRate(calcUtilization(m.getMachineId(), todayStart))
                            .activeUnitId(activeUnitByMachine.get(m.getMachineId()))
                            .faultSince(faultSince)
                            .recoveryEta(null)   // 목표 복구 시각 데이터 없음
                            .build();
                })
                .toList();

        // 2) summary — totalMachineCount == machines.size() 보장
        long total = machines.size();
        long down = machines.stream().filter(x -> STATUS_DOWN.equals(x.getMachineStatus())).count();
        long maintenance = machines.stream().filter(x -> STATUS_MAINTENANCE.equals(x.getMachineStatus())).count();
        long running = machines.stream().filter(x -> STATUS_RUNNING.equals(x.getMachineStatus())).count();
        double avgUtil = total == 0 ? 0.0 : Math.round(((double) running / total) * 1000) / 10.0;

        List<ProcessQueue> queues =
                processQueueRepository.findByDistrict_DistrictIdOrderByQueuePositionAsc(districtId);
        double avgWait = queues.stream()
                .map(ProcessQueue::getActualWaitTime).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).average().orElse(0.0);
        avgWait = Math.round(avgWait * 10) / 10.0;

        long dailyOutput = workStatusRepository.findByMachine_District_DistrictId(districtId).stream()
                .filter(ws -> ws.getStartTime() != null
                        && ws.getStartTime().toLocalDate().isEqual(today)
                        && ws.getOutputQty() != null)
                .mapToLong(WorkStatus::getOutputQty).sum();

        Summary summary = Summary.builder()
                .totalMachineCount(total)
                .availableMachineCount(total - down - maintenance)
                .downMachineCount(down)
                .avgUtilizationRate(avgUtil)
                .totalWaitingUnitCount(queues.size())
                .avgWaitTimeMin(avgWait)
                .dailyOutputQty(dailyOutput)
                .build();

        // 3) stepQueues — step 별 대기 수
        List<StepQueue> stepQueues = queues.stream()
                .filter(q -> q.getStepId() != null)
                .collect(Collectors.groupingBy(ProcessQueue::getStepId, Collectors.counting()))
                .entrySet().stream()
                .map(e -> {
                    ProcessStepOrder step = stepMap.get(e.getKey());
                    return StepQueue.builder()
                            .processStep(step != null ? step.getProcessStep() : null)
                            .stepOrder(step != null ? step.getStepOrder() : null)
                            .waitingUnitCount(e.getValue())
                            .build();
                })
                .sorted(Comparator.comparing(StepQueue::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // 4) reschedule — 진행중 그룹 수 + 최신 재조정
        List<RescheduleGroup> groups = rescheduleGroupRepository.findByDistrictId(districtId);
        long activeCount = groups.stream()
                .filter(g -> GROUP_STATUS_PENDING.equals(g.getGroupStatus())
                        || GROUP_STATUS_APPROVED.equals(g.getGroupStatus()))
                .count();
        LatestReschedule latest = groups.stream()
                .max(Comparator.comparing(RescheduleGroup::getActedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(g -> buildLatestReschedule(g, stepMap, machineByUnitStep))
                .orElse(null);

        return DistrictOverview.builder()
                .districtId(districtId)
                .districtName(district.getDistrictName())
                .summary(summary)
                .machines(machines)
                .stepQueues(stepQueues)
                .rescheduleGroupCount(activeCount)
                .latestReschedule(latest)
                .build();
    }

    private LatestReschedule buildLatestReschedule(RescheduleGroup group,
                                                   Map<String, ProcessStepOrder> stepMap,
                                                   Map<String, String> machineByUnitStep) {
        List<String> memberRiskIds = group.getMemberRiskIds() != null ? group.getMemberRiskIds() : List.of();
        List<DelayRisk> risks = memberRiskIds.isEmpty()
                ? List.of()
                : delayRiskRepository.findByRiskIdIn(memberRiskIds);

        List<RiskItem> riskItems = risks.stream()
                .map(r -> {
                    String unitId = r.getUnit() != null ? r.getUnit().getUnitId() : null;
                    String machineId = (unitId != null && r.getStepId() != null)
                            ? machineByUnitStep.get(unitId + "|" + r.getStepId()) : null;
                    return RiskItem.builder()
                            .riskId(r.getRiskId())
                            .riskLevel(r.getRiskLevel())
                            .detectionTime(r.getDetectionTime())
                            .estimatedDelayHr(r.getEstimatedDelayHr())
                            .delayProbability(r.getDelayProbability())
                            .riskFactor(r.getRiskFactor())
                            .unitId(unitId)
                            .machineId(machineId)
                            .build();
                })
                .toList();

        List<String> affectedUnits = riskItems.stream()
                .map(RiskItem::getUnitId).filter(Objects::nonNull).distinct().toList();

        ProcessStepOrder step = stepMap.get(group.getStepId());
        return LatestReschedule.builder()
                .groupId(group.getGroupId())
                .processStep(step != null ? step.getProcessStep() : null)
                .maxRiskScore(group.getMaxRiskScore())
                .occurredAt(group.getActedAt())
                .rootCauseCategory(rootCauseCategory(group.getRescheduleDetail()))
                .affectedUnits(affectedUnits)
                .delayRisks(riskItems)
                .build();
    }

    /** reschedule_detail.risk_analysis.root_cause.category 안전 추출. */
    @SuppressWarnings("unchecked")
    private String rootCauseCategory(Map<String, Object> detail) {
        if (detail == null) {
            return null;
        }
        Object ra = detail.get("risk_analysis");
        if (!(ra instanceof Map<?, ?> raMap)) {
            return null;
        }
        Object rc = ((Map<String, Object>) raMap).get("root_cause");
        if (!(rc instanceof Map<?, ?> rcMap)) {
            return null;
        }
        Object cat = ((Map<String, Object>) rcMap).get("category");
        return cat != null ? cat.toString() : null;
    }

    private int stepOrderOf(ProcessStepOrder s) {
        return (s != null && s.getStepOrder() != null) ? s.getStepOrder() : Integer.MAX_VALUE;
    }

    /** 금일(시뮬 기준) 가동 시간 비율(%). DistrictMachineServiceImpl 과 동일 산식. */
    private double calcUtilization(String machineId, LocalDateTime todayStart) {
        List<MachineStatusEvent> events = machineStatusEventRepository
                .findByMachineIdAndStartTimeGreaterThanEqual(machineId, todayStart);

        long runningSeconds = events.stream()
                .filter(e -> STATUS_RUNNING.equals(e.getStatus()) && e.getEndTime() != null)
                .mapToLong(e -> Duration.between(e.getStartTime(), e.getEndTime()).getSeconds())
                .sum();

        LocalDateTime maxEnd = events.stream()
                .filter(e -> e.getEndTime() != null)
                .map(MachineStatusEvent::getEndTime)
                .max(LocalDateTime::compareTo).orElse(null);

        if (maxEnd == null || !maxEnd.isAfter(todayStart)) {
            return 0.0;
        }
        long elapsed = Duration.between(todayStart, maxEnd).getSeconds();
        return elapsed == 0 ? 0.0 : Math.round(((double) runningSeconds / elapsed * 100) * 10.0) / 10.0;
    }
}
