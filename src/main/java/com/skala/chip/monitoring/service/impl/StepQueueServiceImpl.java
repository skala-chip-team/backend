package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.dto.StepQueueResponseDTO;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.service.StepQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StepQueueServiceImpl implements StepQueueService {

    private final DistrictRepository districtRepository;
    private final ProcessQueueRepository processQueueRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public StepQueueResponseDTO.DistrictStepQueue getDistrictStepQueues(String districtId) {

        DistrictMaster district = districtRepository.findById(districtId).orElse(null);

        // 공정 단계 정의(process_step, step_order)를 stepId 기준 Map 으로 적재
        Map<String, ProcessStepOrder> stepMap = processStepOrderRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ProcessStepOrder::getStepId,
                        Function.identity(),
                        (a, b) -> a
                ));

        // 해당 구역의 큐를 queue_position 오름차순으로 조회 → stepId 기준 그룹핑
        // (LinkedHashMap 으로 모아 그룹 내 대기 순서를 보존)
        Map<String, List<ProcessQueue>> queuesByStep =
                processQueueRepository.findByDistrict_DistrictIdOrderByQueuePositionAsc(districtId).stream()
                        .filter(q -> q.getStepId() != null)
                        .collect(Collectors.groupingBy(
                                ProcessQueue::getStepId,
                                LinkedHashMap::new,
                                Collectors.toList()
                        ));

        List<StepQueueResponseDTO.StepQueue> steps = queuesByStep.entrySet().stream()
                .map(entry -> {
                    String stepId = entry.getKey();
                    List<ProcessQueue> queues = entry.getValue();
                    ProcessStepOrder stepDef = stepMap.get(stepId);

                    long waitingUnitCount = queues.size();

                    double avgWaitTimeMin = queues.stream()
                            .map(ProcessQueue::getActualWaitTime)
                            .filter(Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(0.0);
                    avgWaitTimeMin = Math.round(avgWaitTimeMin * 10) / 10.0;

                    List<StepQueueResponseDTO.WaitingUnit> waitingUnits = queues.stream()
                            .sorted(Comparator.comparing(
                                    ProcessQueue::getQueuePosition,
                                    Comparator.nullsLast(Comparator.naturalOrder())))
                            .map(this::toWaitingUnit)
                            .toList();

                    return StepQueueResponseDTO.StepQueue.builder()
                            .stepId(stepId)
                            .processStep(stepDef != null ? stepDef.getProcessStep() : null)
                            .stepOrder(stepDef != null ? stepDef.getStepOrder() : null)
                            .waitingUnitCount(waitingUnitCount)
                            .avgWaitTimeMin(avgWaitTimeMin)
                            .waitingUnits(waitingUnits)
                            .build();
                })
                // 공정 순서(step_order) 오름차순 정렬, 정의 없는 step 은 뒤로
                .sorted(Comparator.comparing(
                        StepQueueResponseDTO.StepQueue::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        return StepQueueResponseDTO.DistrictStepQueue.builder()
                .districtId(districtId)
                .districtName(district != null ? district.getDistrictName() : null)
                .steps(steps)
                .build();
    }

    private StepQueueResponseDTO.WaitingUnit toWaitingUnit(ProcessQueue queue) {

        return StepQueueResponseDTO.WaitingUnit.builder()
                .queueId(queue.getQueueId())
                .unitId(
                        queue.getUnit() != null
                                ? queue.getUnit().getUnitId()
                                : null
                )
                .orderId(
                        queue.getUnit() != null
                                ? queue.getUnit().getOrderId()
                                : null
                )
                .unitStatus(
                        queue.getUnit() != null
                                ? queue.getUnit().getUnitStatus()
                                : null
                )
                .queuePosition(queue.getQueuePosition())
                .enqueueTime(queue.getEnqueueTime())
                .actualWaitTime(queue.getActualWaitTime())
                .status(queue.getStatus())
                .build();
    }
}
