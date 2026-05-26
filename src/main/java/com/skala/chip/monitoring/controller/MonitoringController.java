package com.skala.chip.monitoring.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.monitoring.dto.DelayRiskResponseDTO;
import com.skala.chip.monitoring.dto.MachineRequestDTO;
import com.skala.chip.monitoring.dto.MachineResponseDTO;
import com.skala.chip.monitoring.dto.QueueResponseDTO;
import com.skala.chip.monitoring.dto.ScheduleResponseDTO;
import com.skala.chip.monitoring.dto.StatisticsResponseDTO;
import com.skala.chip.monitoring.dto.UnitResponseDTO;
import com.skala.chip.monitoring.dto.WorkStatusResponseDTO;
import com.skala.chip.monitoring.service.DelayRiskService;
import com.skala.chip.monitoring.service.MachineService;
import com.skala.chip.monitoring.service.QueueService;
import com.skala.chip.monitoring.service.ScheduleService;
import com.skala.chip.monitoring.service.StatisticsService;
import com.skala.chip.monitoring.service.UnitService;
import com.skala.chip.monitoring.service.WorkStatusService;
import io.swagger.v3.oas.annotations.Operation;
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
    private final WorkStatusService workStatusService;

    @Operation(summary = "전체 장비 조회")
    @GetMapping("/machines")
    public ApiResponse<List<MachineResponseDTO.MachineInfo>> getMachines() {
        return ApiResponse.success(machineService.getMachines());
    }

    @Operation(summary = "특정 장비 조회")
    @GetMapping("/machines/{machineId}")
    public ApiResponse<MachineResponseDTO.MachineInfo> getMachine(
            @PathVariable String machineId
    ) {
        return ApiResponse.success(machineService.getMachine(machineId));
    }

    @Operation(summary = "장비 상태 변경")
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

    @Operation(summary = "전체 유닛 조회")
    @GetMapping("/units")
    public ApiResponse<List<UnitResponseDTO.UnitInfo>> getUnits() {
        return ApiResponse.success(unitService.getUnits());
    }

    @Operation(summary = "특정 유닛 조회")
    @GetMapping("/units/{unitId}")
    public ApiResponse<UnitResponseDTO.UnitInfo> getUnit(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(unitService.getUnit(unitId));
    }

    @Operation(summary = "전체 스케줄 조회")
    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getSchedules() {
        return ApiResponse.success(scheduleService.getSchedules());
    }

    @Operation(summary = "활성 스케줄 조회")
    @GetMapping("/schedules/active")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getActiveSchedules() {
        return ApiResponse.success(scheduleService.getActiveSchedules());
    }

    @Operation(summary = "장비별 스케줄 조회")
    @GetMapping("/machines/{machineId}/schedules")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getMachineSchedules(
            @PathVariable String machineId
    ) {
        return ApiResponse.success(
                scheduleService.getMachineSchedules(machineId)
        );
    }

    @Operation(summary = "유닛별 스케줄 조회")
    @GetMapping("/units/{unitId}/schedules")
    public ApiResponse<List<ScheduleResponseDTO.ScheduleInfo>> getUnitSchedules(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(
                scheduleService.getUnitSchedules(unitId)
        );
    }

    @Operation(summary = "전체 위험도 조회")
    @GetMapping("/risks")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getRisks() {
        return ApiResponse.success(delayRiskService.getRisks());
    }

    @Operation(summary = "고위험 위험도 조회")
    @GetMapping("/risks/high")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getHighRisks() {
        return ApiResponse.success(delayRiskService.getHighRisks());
    }

    @Operation(summary = "유닛별 위험도 조회")
    @GetMapping("/units/{unitId}/risks")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getUnitRisks(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(
                delayRiskService.getUnitRisks(unitId)
        );
    }

    @Operation(summary = "구역별 위험도 조회")
    @GetMapping("/districts/{districtId}/risks")
    public ApiResponse<List<DelayRiskResponseDTO.RiskInfo>> getDistrictRisks(
            @PathVariable String districtId
    ) {
        return ApiResponse.success(
                delayRiskService.getDistrictRisks(districtId)
        );
    }

    @Operation(summary = "전체 큐 조회")
    @GetMapping("/queues")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getQueues() {
        return ApiResponse.success(queueService.getQueues());
    }

    @Operation(summary = "STEP별 큐 조회")
    @GetMapping("/queues/{stepId}")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getStepQueues(
            @PathVariable String stepId
    ) {
        return ApiResponse.success(
                queueService.getStepQueues(stepId)
        );
    }

    @Operation(summary = "구역별 큐 조회")
    @GetMapping("/districts/{districtId}/queues")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getDistrictQueues(
            @PathVariable String districtId
    ) {
        return ApiResponse.success(
                queueService.getDistrictQueues(districtId)
        );
    }

    @Operation(summary = "유닛별 큐 조회")
    @GetMapping("/units/{unitId}/queues")
    public ApiResponse<List<QueueResponseDTO.QueueInfo>> getUnitQueues(
            @PathVariable String unitId
    ) {
        return ApiResponse.success(
                queueService.getUnitQueues(unitId)
        );
    }

    @Operation(summary = "장비 통계 조회")
    @GetMapping("/statistics/machines")
    public ApiResponse<StatisticsResponseDTO.MachineStatistics> getMachineStatistics() {
        return ApiResponse.success(
                statisticsService.getMachineStatistics()
        );
    }

    @Operation(summary = "전체 작업 상태 조회")
    @GetMapping("/work-status")
    public ApiResponse<List<WorkStatusResponseDTO.WorkStatusInfo>> getWorkStatuses() {

        return ApiResponse.success(
                workStatusService.getWorkStatuses()
        );
    }

    @Operation(summary = "진행 중 작업 상태 조회")
    @GetMapping("/work-status/active")
    public ApiResponse<List<WorkStatusResponseDTO.WorkStatusInfo>> getActiveWorkStatuses() {

        return ApiResponse.success(
                workStatusService.getActiveWorkStatuses()
        );
    }

    @Operation(summary = "장비별 작업 상태 조회")
    @GetMapping("/machines/{machineId}/work-status")
    public ApiResponse<List<WorkStatusResponseDTO.WorkStatusInfo>> getMachineWorkStatuses(
            @PathVariable String machineId
    ) {

        return ApiResponse.success(
                workStatusService.getMachineWorkStatuses(machineId)
        );
    }

    @Operation(summary = "구역별 작업 상태 조회")
    @GetMapping("/districts/{districtId}/work-status")
    public ApiResponse<List<WorkStatusResponseDTO.WorkStatusInfo>> getDistrictWorkStatuses(
            @PathVariable String districtId
    ) {

        return ApiResponse.success(
                workStatusService.getDistrictWorkStatuses(districtId)
        );
    }
}