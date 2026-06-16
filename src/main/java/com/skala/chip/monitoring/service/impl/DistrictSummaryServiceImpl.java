package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.domain.WorkStatus;
import com.skala.chip.monitoring.dto.DistrictSummaryResponseDTO;
import com.skala.chip.monitoring.dto.ProductionStatusResponseDTO;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.WorkStatusRepository;
import com.skala.chip.monitoring.service.DistrictSummaryService;
import com.skala.chip.monitoring.service.SimClock;
import com.skala.chip.order.domain.DailyOrder;
import com.skala.chip.order.repository.DailyOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DistrictSummaryServiceImpl implements DistrictSummaryService {

    // 장비 상태 문자열 상수 (machine_master.machine_status 실제 값 기준)
    private static final String STATUS_RUNNING = "가동";
    private static final String STATUS_MAINTENANCE = "점검중";
    private static final String STATUS_DOWN = "정지";

    private final DistrictRepository districtRepository;
    private final MachineRepository machineRepository;
    private final ProcessQueueRepository processQueueRepository;
    private final WorkStatusRepository workStatusRepository;
    private final DailyOrderRepository dailyOrderRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final SimClock simClock;

    @Override
    @Transactional(readOnly = true)
    public DistrictSummaryResponseDTO.DistrictSummary getDistrictSummary(String districtId) {

        DistrictMaster district = districtRepository.findById(districtId).orElse(null);

        // 1. 장비 현황 집계
        long totalMachine = machineRepository.countByDistrict_DistrictId(districtId);
        long running = machineRepository
                .countByDistrict_DistrictIdAndMachineStatus(districtId, STATUS_RUNNING);
        long maintenance = machineRepository
                .countByDistrict_DistrictIdAndMachineStatus(districtId, STATUS_MAINTENANCE);
        long down = machineRepository
                .countByDistrict_DistrictIdAndMachineStatus(districtId, STATUS_DOWN);

        // 가용 장비 = 전체 - 장애 - 점검중 (= 가동중 + 대기)
        long available = totalMachine - down - maintenance;

        // 평균 가동률 = 가동중 / 전체 (소수점 첫째자리, %)
        double avgUtilizationRate = totalMachine == 0
                ? 0.0
                : Math.round(((double) running / totalMachine) * 1000) / 10.0;

        // 2. 대기 unit 현황 집계
        List<ProcessQueue> queues =
                processQueueRepository.findByDistrict_DistrictIdOrderByQueuePositionAsc(districtId);

        long totalWaitingUnitCount = queues.size();

        double avgWaitTimeMin = queues.stream()
                .map(ProcessQueue::getActualWaitTime)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        avgWaitTimeMin = Math.round(avgWaitTimeMin * 10) / 10.0;

        // 3. 금일 생산량(완성품) 집계.
        // 시뮬레이션 데이터는 sim 달력 기준이라 실제 LocalDate.now() 가 아닌 sim 기준 "오늘" 을 쓴다.
        // 한 unit 은 여러 공정(step)을 거치며 step 마다 work_status 가 생기므로, 전 공정의 output_qty 를
        // 합치면 완성품 수가 (공정 수)배로 부풀려진다(예: unit 당 ~3배). 따라서 "최종 공정(step_order
        // 최대)" 의 output 만 합산해 실제 완성 수량을 구한다.
        LocalDateTime simNow = simClock.now();
        LocalDate today = simNow.toLocalDate();
        String finalStepId = processStepOrderRepository.findAll().stream()
                .filter(s -> s.getStepOrder() != null)
                .max(Comparator.comparingInt(ProcessStepOrder::getStepOrder))
                .map(ProcessStepOrder::getStepId)
                .orElse(null);
        long dailyOutputQty = finalStepId == null ? 0L
                : workStatusRepository.sumFinalStepOutput(
                        districtId,
                        today.atStartOfDay(),
                        today.plusDays(1).atStartOfDay(),
                        finalStepId);

        // 4. 금일 생산 목표량 집계 (plan_date 가 오늘인 해당 구역 주문의 planned_output_qty 합)
        long dailyTargetOutputQty = dailyOrderRepository
                .findByDistrict_DistrictIdAndPlanDate(districtId, today).stream()
                .map(DailyOrder::getPlannedOutputQty)
                .filter(Objects::nonNull)
                .mapToLong(Integer::longValue)
                .sum();

        // 달성률: 목표량이 있을 때만 산출. 목표 정보가 없으면(0) null = "달성률 산출 불가"(절대값만 표시).
        Double achievementRate = dailyTargetOutputQty > 0
                ? Math.round(((double) dailyOutputQty / dailyTargetOutputQty) * 1000) / 10.0
                : null;

        return DistrictSummaryResponseDTO.DistrictSummary.builder()
                .districtId(districtId)
                .districtName(district != null ? district.getDistrictName() : null)
                .simulatedAt(simNow)
                .totalMachineCount(totalMachine)
                .availableMachineCount(available)
                .downMachineCount(down)
                .avgUtilizationRate(avgUtilizationRate)
                .totalWaitingUnitCount(totalWaitingUnitCount)
                .avgWaitTimeMin(avgWaitTimeMin)
                .dailyOutputQty(dailyOutputQty)
                .dailyTargetOutputQty(dailyTargetOutputQty)
                .achievementRate(achievementRate)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionStatusResponseDTO getProductionStatus() {
        LocalDateTime simNow = simClock.now();
        LocalDate today = simNow.toLocalDate();
        String finalStepId = processStepOrderRepository.findAll().stream()
                .filter(s -> s.getStepOrder() != null)
                .max(Comparator.comparingInt(ProcessStepOrder::getStepOrder))
                .map(ProcessStepOrder::getStepId)
                .orElse(null);
        if (finalStepId == null) {
            return new ProductionStatusResponseDTO(0L, null, today, simNow);
        }
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        long completed = workStatusRepository.sumFinalStepOutputAll(dayStart, dayEnd, finalStepId);
        LocalDateTime latest = workStatusRepository.latestFinalStepAt(dayStart, dayEnd, finalStepId);
        return new ProductionStatusResponseDTO(completed, latest, today, simNow);
    }
}
