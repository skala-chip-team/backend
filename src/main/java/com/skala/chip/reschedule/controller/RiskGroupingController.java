package com.skala.chip.reschedule.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.reschedule.dto.OrchestrationResponse;
import com.skala.chip.reschedule.dto.RiskGroupingResponse;
import com.skala.chip.reschedule.service.RescheduleOrchestrationService;
import com.skala.chip.reschedule.service.RiskGroupingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reschedule")
public class RiskGroupingController {

    private final RiskGroupingService riskGroupingService;
    private final RescheduleOrchestrationService orchestrationService;

    /**
     * 위험 그룹핑 트리거. detection_time 미지정 시 최신 배치를 대상으로 한다.
     * 에이전트 호출 대상(triggered=true) step 그룹을 포함한 그룹 목록을 반환한다.
     */
    @PostMapping("/risk-groups")
    public ApiResponse<RiskGroupingResponse> groupRisks(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime detectionTime
    ) {
        return ApiResponse.success(riskGroupingService.groupRisks(detectionTime));
    }

    /**
     * 재조정 전체 흐름 트리거: 모델 실행(/predict) → 그룹핑/탐지 → 임계값 초과 그룹마다
     * 에이전트 호출(/run)로 재조정안(reschedule_detail)까지 생성한다.
     * snapTime 미지정 시 최신 delay_risk 시각을 사용한다.
     */
    @PostMapping("/run")
    public ApiResponse<OrchestrationResponse> run(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime snapTime
    ) {
        return ApiResponse.success(orchestrationService.orchestrate(snapTime));
    }
}
