package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.dto.QueueResponseDTO;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final ProcessQueueRepository processQueueRepository;

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponseDTO.QueueInfo> getQueues() {

        List<ProcessQueue> queues = processQueueRepository.findAll();

        return queues.stream()
                .map(QueueResponseDTO.QueueInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponseDTO.QueueInfo> getStepQueues(String stepId) {

        List<ProcessQueue> queues =
                processQueueRepository
                        .findByStepIdOrderByQueuePositionAsc(stepId);

        return queues.stream()
                .map(QueueResponseDTO.QueueInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponseDTO.QueueInfo> getDistrictQueues(String districtId) {

        List<ProcessQueue> queues =
                processQueueRepository
                        .findByDistrict_DistrictIdOrderByQueuePositionAsc(districtId);

        return queues.stream()
                .map(QueueResponseDTO.QueueInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponseDTO.QueueInfo> getUnitQueues(String unitId) {

        List<ProcessQueue> queues =
                processQueueRepository.findByUnit_UnitId(unitId);

        return queues.stream()
                .map(QueueResponseDTO.QueueInfo::from)
                .toList();
    }
}