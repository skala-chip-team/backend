package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.MachineMaster;
import com.skala.chip.monitoring.dto.MachineResponseDTO;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MachineServiceImpl implements MachineService {

    private final MachineRepository machineRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MachineResponseDTO.MachineInfo> getMachines() {
        List<MachineMaster> machines = machineRepository.findAll();

        return machines.stream()
                .map(MachineResponseDTO.MachineInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MachineResponseDTO.MachineInfo getMachine(String machineId) {
        MachineMaster machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장비를 찾을 수 없습니다. machineId=" + machineId)
                );

        return MachineResponseDTO.MachineInfo.from(machine);
    }

    @Override
    @Transactional
    public MachineResponseDTO.MachineInfo updateMachineStatus(
            String machineId,
            String machineStatus
    ) {
        MachineMaster machine = machineRepository.findById(machineId)
                .orElseThrow(() ->
                        new IllegalArgumentException("장비를 찾을 수 없습니다. machineId=" + machineId)
                );

        machine.updateStatus(machineStatus);

        return MachineResponseDTO.MachineInfo.from(machine);
    }
}