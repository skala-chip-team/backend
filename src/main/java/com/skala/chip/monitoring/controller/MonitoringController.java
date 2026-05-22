package com.skala.chip.monitoring.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.monitoring.dto.MachineRequestDTO;
import com.skala.chip.monitoring.dto.MachineResponseDTO;
import com.skala.chip.monitoring.service.MachineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MachineService machineService;

    @GetMapping("/machines")
    public ApiResponse<List<MachineResponseDTO.MachineInfo>> getMachines() {
        return ApiResponse.success(machineService.getMachines());
    }

    @GetMapping("/machines/{machineId}")
    public ApiResponse<MachineResponseDTO.MachineInfo> getMachine(
            @PathVariable String machineId
    ) {
        return ApiResponse.success(machineService.getMachine(machineId));
    }

    @PatchMapping("/machines/{machineId}/status")
    public ApiResponse<MachineResponseDTO.MachineInfo> updateMachineStatus(
            @PathVariable String machineId,
            @RequestBody MachineRequestDTO.UpdateStatusRequest request
    ) {
        return ApiResponse.success(
                machineService.updateMachineStatus(
                        machineId,
                        request.getMachineStatus()
                )
        );
    }
}