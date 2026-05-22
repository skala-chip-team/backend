package com.skala.chip.monitoring.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.monitoring.dto.DelayRiskResponseDTO;
import com.skala.chip.monitoring.dto.MachineRequestDTO;
import com.skala.chip.monitoring.dto.MachineResponseDTO;
import com.skala.chip.monitoring.dto.QueueResponseDTO;
import com.skala.chip.monitoring.dto.ScheduleResponseDTO;
import com.skala.chip.monitoring.dto.StatisticsResponseDTO;
import com.skala.chip.monitoring.dto.UnitResponseDTO;
import com.skala.chip.monitoring.service.DelayRiskService;
import com.skala.chip.monitoring.service.MachineService;
import com.skala.chip.monitoring.service.QueueService;
import com.skala.chip.monitoring.service.ScheduleService;
import com.skala.chip.monitoring.service.StatisticsService;
import com.skala.chip.monitoring.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MachineService machineService;
    private final UnitService unitService;
    private final ScheduleService scheduleService;
    private final DelayRiskService delayRiskService;
    private final QueueService queueService;
    private final StatisticsService statisticsService;

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

    @GetMapping("/units")
    public ApiResponse<List<UnitResponseDTO.UnitInfo>> getUnits() {
        return ApiResponse.success(unitService.getUnits());
    }

    @GetMapping("/units/{unitId}")
    public ApiResponse<UnitResponseDTO.UnitInfo> getUnit(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(unitService.getUnit(unitId));
    }

    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getSchedules() {
        return ApiResponse.success(scheduleService.getSchedules());
    }

    @GetMapping("/schedules/active")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getActiveSchedules() {
        return ApiResponse.success(scheduleService.getActiveSchedules());
    }

    @GetMapping("/machines/{machineId}/schedules")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getMachineSchedules(
            @PathVariable String machineId
    ) {
        return ApiResponse.success(
                scheduleService.getMachineSchedules(machineId)
        );
    }

    @GetMapping("/units/{unitId}/schedules")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getUnitSchedules(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(
                scheduleService.getUnitSchedules(unitId)
        );
    }

    @GetMapping("/risks")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getRisks() {
        return ApiResponse.success(delayRiskService.getRisks());
    }

    @GetMapping("/risks/high")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getHighRisks() {
        return ApiResponse.success(delayRiskService.getHighRisks());
    }

    @GetMapping("/units/{unitId}/risks")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getUnitRisks(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(
                delayRiskService.getUnitRisks(unitId)
        );
    }

    @GetMapping("/districts/{districtId}/risks")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getDistrictRisks(
            @PathVariable String districtId
    ) {
        return ApiResponse.success(
                delayRiskService.getDistrictRisks(districtId)
        );
    }

    @GetMapping("/queues")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getQueues() {
        return ApiResponse.success(queueService.getQueues());
    }

    @GetMapping("/queues/{stepId}")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getStepQueues(
            @PathVariable String stepId
    ) {
        return ApiResponse.success(
                queueService.getStepQueues(stepId)
        );
    }

    @GetMapping("/districts/{districtId}/queues")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getDistrictQueues(
            @PathVariable String districtId
    ) {
        return ApiResponse.success(
                queueService.getDistrictQueues(districtId)
        );
    }

    @GetMapping("/units/{unitId}/queues")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getUnitQueues(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(
                queueService.getUnitQueues(unitId)
        );
    }

    @GetMapping("/statistics/machines")
    public ApiResponse<StatisticsResponseDTO.MachineStatistics> getMachineStatistics() {
        return ApiResponse.success(
                statisticsService.getMachineStatistics()
        );
    }
}