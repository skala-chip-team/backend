package com.skala.chip.order.service.impl;

import com.skala.chip.exception.custom.OrderNotFoundException;
import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.domain.UnitMaster;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.repository.UnitRepository;
import com.skala.chip.monitoring.service.SimClock;
import com.skala.chip.order.domain.DailyOrder;
import com.skala.chip.order.dto.OrderResponseDTO;
import com.skala.chip.order.repository.DailyOrderRepository;
import com.skala.chip.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final String STATUS_WAITING = "대기";
    private static final String STATUS_IN_PROGRESS = "진행중";
    private static final String STATUS_DONE = "완료";

    private final DailyOrderRepository dailyOrderRepository;
    private final UnitRepository unitRepository;
    private final ProcessQueueRepository processQueueRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final ScheduleRepository scheduleRepository;
    private final SimClock simClock;

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO.OrderList getOrders(String status, String districtId) {

        LocalDate today = simClock.now().toLocalDate();

        // 전체 유닛을 orderId 로 그룹핑(메모리 집계) — StepQueueServiceImpl 과 동일 스타일
        Map<String, List<UnitMaster>> unitsByOrder = unitRepository.findAll().stream()
                .filter(u -> u.getOrderId() != null)
                .collect(Collectors.groupingBy(UnitMaster::getOrderId));

        List<OrderResponseDTO.OrderSummary> orders = dailyOrderRepository.findAllByOrderByDueDateAsc().stream()
                .map(order -> toSummary(
                        order,
                        unitsByOrder.getOrDefault(order.getOrderId(), List.of()),
                        today
                ))
                // 옵션 필터 적용
                .filter(s -> status == null || status.equals(s.getStatus()))
                .filter(s -> districtId == null || districtId.equals(s.getDistrictId()))
                .toList();

        long imminentCount = orders.stream().filter(OrderResponseDTO.OrderSummary::isDueImminent).count();

        return OrderResponseDTO.OrderList.builder()
                .totalCount(orders.size())
                .imminentCount(imminentCount)
                .orders(orders)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO.OrderDetail getOrderDetail(String orderId) {

        DailyOrder order = dailyOrderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        LocalDate today = simClock.now().toLocalDate();
        List<UnitMaster> units = unitRepository.findByOrderId(orderId);

        // 공정 단계 정의(step_order 오름차순) — 모든 유닛 타임라인이 공유
        List<ProcessStepOrder> stepDefs = processStepOrderRepository.findAll().stream()
                .sorted(Comparator.comparing(
                        ProcessStepOrder::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        Map<String, ProcessStepOrder> stepMap = stepDefs.stream()
                .collect(Collectors.toMap(ProcessStepOrder::getStepId, Function.identity(), (a, b) -> a));

        int totalUnits = units.size();
        int completedUnits = (int) units.stream()
                .filter(u -> STATUS_DONE.equals(u.getUnitStatus()))
                .count();
        String orderStatus = deriveOrderStatus(units, completedUnits);

        List<OrderResponseDTO.UnitProgress> unitProgresses = units.stream()
                .map(u -> toUnitProgress(u, stepDefs, stepMap))
                .toList();

        DistrictMaster district = order.getDistrict();

        return OrderResponseDTO.OrderDetail.builder()
                .orderId(order.getOrderId())
                .districtId(district != null ? district.getDistrictId() : null)
                .districtName(district != null ? district.getDistrictName() : null)
                .planDate(order.getPlanDate())
                .dueDate(order.getDueDate())
                .plannedOutputQty(order.getPlannedOutputQty())
                .priority(order.getOrderPriority())
                .priorityLabel(priorityLabel(order.getOrderPriority()))
                .status(orderStatus)
                .totalUnits(totalUnits)
                .completedUnits(completedUnits)
                .progressRatio(ratio(completedUnits, totalUnits))
                .dueImminent(isDueImminent(order.getDueDate(), today))
                .urgent(Boolean.TRUE.equals(order.getIsBurst()))
                .units(unitProgresses)
                .build();
    }

    // ----- summary -----

    private OrderResponseDTO.OrderSummary toSummary(DailyOrder order, List<UnitMaster> units, LocalDate today) {

        int totalUnits = units.size();
        int completedUnits = (int) units.stream()
                .filter(u -> STATUS_DONE.equals(u.getUnitStatus()))
                .count();
        DistrictMaster district = order.getDistrict();

        return OrderResponseDTO.OrderSummary.builder()
                .orderId(order.getOrderId())
                .districtId(district != null ? district.getDistrictId() : null)
                .districtName(district != null ? district.getDistrictName() : null)
                .planDate(order.getPlanDate())
                .dueDate(order.getDueDate())
                .plannedOutputQty(order.getPlannedOutputQty())
                .priority(order.getOrderPriority())
                .priorityLabel(priorityLabel(order.getOrderPriority()))
                .status(deriveOrderStatus(units, completedUnits))
                .totalUnits(totalUnits)
                .completedUnits(completedUnits)
                .progressRatio(ratio(completedUnits, totalUnits))
                .dueImminent(isDueImminent(order.getDueDate(), today))
                .urgent(Boolean.TRUE.equals(order.getIsBurst()))
                .build();
    }

    /**
     * 유닛 unit_status 집계로 주문 상태 유도.
     * 전부 완료 → 완료, 하나라도 진행중이거나 일부만 완료 → 진행중, 그 외 → 대기.
     */
    private String deriveOrderStatus(List<UnitMaster> units, int completedUnits) {
        if (units.isEmpty()) {
            return STATUS_WAITING;
        }
        if (completedUnits == units.size()) {
            return STATUS_DONE;
        }
        boolean anyInProgress = units.stream()
                .anyMatch(u -> STATUS_IN_PROGRESS.equals(u.getUnitStatus()));
        if (anyInProgress || completedUnits > 0) {
            return STATUS_IN_PROGRESS;
        }
        return STATUS_WAITING;
    }

    // ----- unit timeline -----

    private OrderResponseDTO.UnitProgress toUnitProgress(
            UnitMaster unit,
            List<ProcessStepOrder> stepDefs,
            Map<String, ProcessStepOrder> stepMap) {

        boolean unitDone = STATUS_DONE.equals(unit.getUnitStatus());

        List<ScheduleMaster> schedules = scheduleRepository.findByUnit_UnitId(unit.getUnitId());

        // 현재 위치: 진행 중인 active 스케줄 우선. process_queue 는 작업 시작 시 행이 삭제되므로
        // 진행 중 유닛은 스케줄에서 잡아야 안정적이다. 대기 중이면 process_queue 최신 enqueue 행으로 폴백.
        String currentStepId = null;
        String currentMachineId = null;
        LocalDateTime estimatedCompleteTime = null;

        Optional<ScheduleMaster> inProgress = schedules.stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive())
                        && STATUS_IN_PROGRESS.equals(s.getStatus()))
                .findFirst();

        if (inProgress.isPresent()) {
            ScheduleMaster s = inProgress.get();
            currentStepId = s.getStepId();
            currentMachineId = s.getMachine() != null ? s.getMachine().getMachineId() : null;
            estimatedCompleteTime = estimateComplete(s.getEstimatedStart(), stepMap.get(currentStepId));
        } else if (!unitDone) {
            currentStepId = processQueueRepository.findByUnit_UnitId(unit.getUnitId()).stream()
                    .filter(q -> q.getEnqueueTime() != null)
                    .max(Comparator.comparing(ProcessQueue::getEnqueueTime))
                    .map(ProcessQueue::getStepId)
                    .orElse(null);
            final String csid = currentStepId;
            if (csid != null) {
                Optional<ScheduleMaster> activeForStep = schedules.stream()
                        .filter(s -> Boolean.TRUE.equals(s.getActive()) && csid.equals(s.getStepId()))
                        .findFirst();
                if (activeForStep.isPresent()) {
                    ScheduleMaster s = activeForStep.get();
                    currentMachineId = s.getMachine() != null ? s.getMachine().getMachineId() : null;
                    estimatedCompleteTime = estimateComplete(s.getEstimatedStart(), stepMap.get(csid));
                }
            }
        }

        Integer currentStepOrder = (currentStepId != null && stepMap.containsKey(currentStepId))
                ? stepMap.get(currentStepId).getStepOrder()
                : null;

        List<OrderResponseDTO.StepProgress> steps = buildSteps(stepDefs, unitDone, currentStepOrder);

        return OrderResponseDTO.UnitProgress.builder()
                .unitId(unit.getUnitId())
                .unitSizeQty(unit.getUnitSizeQty())
                .unitStatus(unit.getUnitStatus())
                .actualStartTime(unit.getActualStartTime())
                .actualCompleteTime(unit.getActualCompleteTime())
                .currentStepId(currentStepId)
                .currentMachineId(currentMachineId)
                .estimatedCompleteTime(estimatedCompleteTime)
                .steps(steps)
                .build();
    }

    private List<OrderResponseDTO.StepProgress> buildSteps(
            List<ProcessStepOrder> stepDefs,
            boolean unitDone,
            Integer currentStepOrder) {

        return stepDefs.stream()
                .map(def -> OrderResponseDTO.StepProgress.builder()
                        .stepId(def.getStepId())
                        .processStep(def.getProcessStep())
                        .stepOrder(def.getStepOrder())
                        .stepStatus(stepStatus(def.getStepOrder(), unitDone, currentStepOrder))
                        .build())
                .toList();
    }

    private String stepStatus(Integer stepOrder, boolean unitDone, Integer currentStepOrder) {
        if (unitDone) {
            return "done";
        }
        if (currentStepOrder == null || stepOrder == null) {
            return "pending";
        }
        if (stepOrder < currentStepOrder) {
            return "done";
        }
        if (stepOrder.equals(currentStepOrder)) {
            return "current";
        }
        return "pending";
    }

    private LocalDateTime estimateComplete(LocalDateTime estimatedStart, ProcessStepOrder stepDef) {
        if (estimatedStart == null || stepDef == null || stepDef.getStepAvgTime() == null) {
            return null;
        }
        return estimatedStart.plusMinutes(stepDef.getStepAvgTime());
    }

    // ----- helpers -----

    private boolean isDueImminent(LocalDateTime dueDate, LocalDate today) {
        return dueDate != null && dueDate.toLocalDate().isEqual(today);
    }

    private double ratio(int completed, int total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.round((double) completed / total * 1000) / 1000.0;
    }

    private String priorityLabel(Integer priority) {
        if (priority == null) {
            return null;
        }
        return switch (priority) {
            case 1 -> "매우 높음";
            case 2 -> "높음";
            case 3 -> "보통";
            case 4 -> "낮음";
            case 5 -> "매우 낮음";
            default -> "보통";
        };
    }
}
