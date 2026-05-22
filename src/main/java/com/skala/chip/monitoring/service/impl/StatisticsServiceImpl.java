package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.dto.StatisticsResponseDTO;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final MachineRepository machineRepository;

    @Override
    @Transactional(readOnly = true)
    public StatisticsResponseDTO.MachineStatistics getMachineStatistics() {
        long total = machineRepository.count();

        long running = machineRepository.countByMachineStatus("가동중");
        long idle = machineRepository.countByMachineStatus("대기");
        long maintenance = machineRepository.countByMachineStatus("점검중");
        long down = machineRepository.countByMachineStatus("장애");

        double utilizationRate = total == 0
                ? 0.0
                : Math.round(((double) running / total) * 1000) / 10.0;

        return StatisticsResponseDTO.MachineStatistics.builder()
                .totalMachines(total)
                .runningMachines(running)
                .idleMachines(idle)
                .maintenanceMachines(maintenance)
                .downMachines(down)
                .utilizationRate(utilizationRate)
                .build();
    }
}