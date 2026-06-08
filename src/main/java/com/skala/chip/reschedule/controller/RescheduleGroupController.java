package com.skala.chip.reschedule.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupSummaryResponse;
import com.skala.chip.reschedule.dto.RescheduleSelectionResponse;
import com.skala.chip.reschedule.dto.SelectRescheduleRequest;
import com.skala.chip.reschedule.service.RescheduleGroupService;
import com.skala.chip.reschedule.service.RescheduleOrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reschedule/groups")
public class RescheduleGroupController {

    private final RescheduleGroupService rescheduleGroupService;
    private final RescheduleOrchestrationService orchestrationService;

    /**
     * 재조정안 관리(목록) 페이지.
     * @param districtId 구역 필터 (없으면 전체)
     * @param status     상태 필터 (active=진행중 / expired=만료 / pending / approved, 없으면 전체)
     */
    @GetMapping
    public ApiResponse<List<RescheduleGroupSummaryResponse>> getGroups(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(rescheduleGroupService.getGroups(districtId, status));
    }

    /**
     * 재조정 상세 페이지: 그룹 정보 + 관련 delay_risks + 에이전트 출력(reschedule_detail).
     */
    @GetMapping("/{groupId}")
    public ApiResponse<RescheduleGroupDetailResponse> getGroupDetail(@PathVariable String groupId) {
        return ApiResponse.success(rescheduleGroupService.getGroupDetail(groupId));
    }

    /**
     * 재조정안 (재)생성: 해당 그룹의 대표 위험으로 에이전트(/run)를 호출해
     * reschedule_detail 을 채운다. 에이전트 실패나 재시도 시 수동으로 호출.
     */
    @PostMapping("/{groupId}/generate")
    public ApiResponse<RescheduleGroupDetailResponse> generateDetail(@PathVariable String groupId) {
        return ApiResponse.success(orchestrationService.generateForGroup(groupId));
    }

    /**
     * 재조정 전략 선택·확정: reschedule_group 에서 선택 전략을 selected_yn=true 로 표시하고,
     * queue_reorder 를 process_queue 에 반영, group_status 를 approved 로 변경.
     */
    @PostMapping("/{groupId}/select")
    public ApiResponse<RescheduleSelectionResponse> selectStrategy(
            @PathVariable String groupId,
            @RequestBody SelectRescheduleRequest request
    ) {
        return ApiResponse.success(rescheduleGroupService.selectStrategy(groupId, request));
    }
}
