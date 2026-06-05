package com.skala.chip.reschedule.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.reschedule.dto.RiskGroupingResponse;
import com.skala.chip.reschedule.dto.RiskThresholdResponse;
import com.skala.chip.reschedule.dto.RiskThresholdUpdateRequest;
import com.skala.chip.reschedule.service.RescheduleConfigService;
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
    private final RescheduleConfigService configService;

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
     * 현재 위험 그룹핑 임계값 조회.
     */
    @GetMapping("/config/risk-threshold")
    public ApiResponse<RiskThresholdResponse> getRiskThreshold() {
        return ApiResponse.success(new RiskThresholdResponse(configService.getRiskThreshold()));
    }

    /**
     * 위험 그룹핑 임계값 변경 (0.0 ~ 1.0).
     */
    @PutMapping("/config/risk-threshold")
    public ApiResponse<RiskThresholdResponse> updateRiskThreshold(
            @RequestBody RiskThresholdUpdateRequest request
    ) {
        if (request.value() == null) {
            throw new IllegalArgumentException("value 는 필수입니다.");
        }
        return ApiResponse.success(new RiskThresholdResponse(
                configService.updateRiskThreshold(request.value())
        ));
    }
}
