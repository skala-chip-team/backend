package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.StatisticsResponseDTO;

public interface StatisticsService {

    StatisticsResponseDTO.MachineStatistics getMachineStatistics();
}