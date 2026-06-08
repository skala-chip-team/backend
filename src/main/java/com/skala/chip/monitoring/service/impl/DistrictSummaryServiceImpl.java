package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.domain.WorkStatus;
import com.skala.chip.monitoring.dto.DistrictSummaryResponseDTO;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.WorkStatusRepository;
import com.skala.chip.monitoring.service.DistrictSummaryService;
import com.skala.chip.monitoring.service.SimClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

        // 3. 금일 생산량 집계 (오늘 시작된 작업의 output_qty 합계)
        // 시뮬레이션 데이터는 sim 달력 기준이라 실제 LocalDate.now() 가 아닌 sim 기준 "오늘" 을 쓴다.
        LocalDate today = simClock.now().toLocalDate();
        long dailyOutputQty = workStatusRepository
                .findByMachine_District_DistrictId(districtId).stream()
                .filter(ws -> ws.getStartTime() != null
                        && ws.getStartTime().toLocalDate().isEqual(today)
                        && ws.getOutputQty() != null)
                .mapToLong(WorkStatus::getOutputQty)
                .sum();

        return DistrictSummaryResponseDTO.DistrictSummary.builder()
                .districtId(districtId)
                .districtName(district != null ? district.getDistrictName() : null)
                .totalMachineCount(totalMachine)
                .availableMachineCount(available)
                .downMachineCount(down)
                .avgUtilizationRate(avgUtilizationRate)
                .totalWaitingUnitCount(totalWaitingUnitCount)
                .avgWaitTimeMin(avgWaitTimeMin)
                .dailyOutputQty(dailyOutputQty)
                .build();
    }
}
