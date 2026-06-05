package com.skala.chip.reschedule.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.RescheduleSelectionResponse;
import com.skala.chip.reschedule.dto.SelectRescheduleRequest;
import com.skala.chip.reschedule.service.RescheduleGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reschedule/groups")
public class RescheduleGroupController {

    private final RescheduleGroupService rescheduleGroupService;

    /**
     * 재조정 상세 페이지: 그룹 정보 + 관련 delay_risks + 에이전트 출력(reschedule_detail).
     */
    @GetMapping("/{groupId}")
    public ApiResponse<RescheduleGroupDetailResponse> getGroupDetail(@PathVariable String groupId) {
        return ApiResponse.success(rescheduleGroupService.getGroupDetail(groupId));
    }

    /**
     * 재조정 전략 선택·확정: 선택된 전략을 reschedule_selection 에 저장하고 그룹을 approved 로 변경.
     */
    @PostMapping("/{groupId}/select")
    public ApiResponse<RescheduleSelectionResponse> selectStrategy(
            @PathVariable String groupId,
            @RequestBody SelectRescheduleRequest request
    ) {
        return ApiResponse.success(rescheduleGroupService.selectStrategy(groupId, request));
    }
}
