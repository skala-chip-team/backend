package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.StepQueueResponseDTO;

public interface StepQueueService {

    StepQueueResponseDTO.DistrictStepQueue getDistrictStepQueues(String districtId);
}
