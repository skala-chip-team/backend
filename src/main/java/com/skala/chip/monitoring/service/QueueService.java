package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.QueueResponseDTO;

import java.util.List;

public interface QueueService {

    List<QueueResponseDTO.QueueInfo> getQueues();

    List<QueueResponseDTO.QueueInfo> getStepQueues(String stepId);

    List<QueueResponseDTO.QueueInfo> getDistrictQueues(String districtId);

    List<QueueResponseDTO.QueueInfo> getUnitQueues(String unitId);
}