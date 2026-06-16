package com.skala.chip.reschedule.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupSummaryResponse;
import com.skala.chip.reschedule.dto.RescheduleHistoryResponse;
import com.skala.chip.reschedule.dto.RescheduleSelectionResponse;
import com.skala.chip.reschedule.dto.SelectRescheduleRequest;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import com.skala.chip.reschedule.service.RescheduleGroupService;
import com.skala.chip.reschedule.service.RescheduleOrchestrationService;
import com.skala.chip.user.service.DistrictAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reschedule/groups")
public class RescheduleGroupController {

    private final RescheduleGroupService rescheduleGroupService;
    private final RescheduleOrchestrationService orchestrationService;
    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final DistrictAccessGuard districtAccessGuard;

    /** 그룹의 구역을 사용자 권한과 대조한다. 그룹이 없으면(구역 판별 불가) 검증을 생략한다(존재성은 후속 책임). */
    private void assertGroupAccess(String email, String groupId) {
        String districtId = rescheduleGroupRepository.findById(groupId)
                .map(RescheduleGroup::getDistrictId)
                .orElse(null);
        if (districtId != null) {
            districtAccessGuard.assertDistrict(email, districtId);
        }
    }

    /**
     * 재조정안 관리(목록) 페이지.
     * @param districtId 구역 필터 (없으면 전체)
     * @param status     상태 필터 (active=진행중 / expired=만료 / pending / approved, 없으면 전체)
     */
    @GetMapping
    public ApiResponse<List<RescheduleGroupSummaryResponse>> getGroups(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal String email
    ) {
        if (districtId != null) {
            districtAccessGuard.assertDistrict(email, districtId);
            return ApiResponse.success(rescheduleGroupService.getGroups(districtId, status));
        }
        // 구역 미지정: 담당 구역만 노출 (ADMIN 은 전체)
        Set<String> allowed = districtAccessGuard.allowedDistrictIds(email);
        List<RescheduleGroupSummaryResponse> groups = rescheduleGroupService.getGroups(null, status);
        if (allowed != null) {
            groups = groups.stream()
                    .filter(g -> allowed.contains(g.districtId()))
                    .toList();
        }
        return ApiResponse.success(groups);
    }

    /**
     * 기간별 재조정 이력 조회 (페이지네이션).
     * @param from/to 조회 기간(yyyy-MM-dd, 포함). 최대 92일.
     * @param districtId 구역 필터(선택). 없으면 담당 구역 전체(ADMIN 은 전체).
     */
    @GetMapping("/history")
    public ApiResponse<RescheduleHistoryResponse> getHistory(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String districtId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal String email
    ) {
        if (districtId != null && !districtId.isBlank()) {
            districtAccessGuard.assertDistrict(email, districtId);
            return ApiResponse.success(
                    rescheduleGroupService.getHistory(districtId, null, from, to, page, size));
        }
        Set<String> allowed = districtAccessGuard.allowedDistrictIds(email); // null = ADMIN(전체)
        return ApiResponse.success(
                rescheduleGroupService.getHistory(null, allowed, from, to, page, size));
    }

    /**
     * 재조정 상세 페이지: 그룹 정보 + 관련 delay_risks + 에이전트 출력(reschedule_detail).
     */
    @GetMapping("/{groupId}")
    public ApiResponse<RescheduleGroupDetailResponse> getGroupDetail(
            @PathVariable String groupId,
            @AuthenticationPrincipal String email
    ) {
        assertGroupAccess(email, groupId);
        return ApiResponse.success(rescheduleGroupService.getGroupDetail(groupId));
    }

    /**
     * 재조정안 (재)생성: 해당 그룹의 대표 위험으로 에이전트(/run)를 호출해
     * reschedule_detail 을 채운다. 에이전트 실패나 재시도 시 수동으로 호출.
     */
    @PostMapping("/{groupId}/generate")
    public ApiResponse<RescheduleGroupDetailResponse> generateDetail(
            @PathVariable String groupId,
            @AuthenticationPrincipal String email
    ) {
        assertGroupAccess(email, groupId);
        return ApiResponse.success(orchestrationService.generateForGroup(groupId));
    }

    /**
     * 재조정 전략 선택·확정: reschedule_group 에서 선택 전략을 selected_yn=true 로 표시하고,
     * queue_reorder 를 process_queue 에 반영, group_status 를 approved 로 변경.
     */
    @PostMapping("/{groupId}/select")
    public ApiResponse<RescheduleSelectionResponse> selectStrategy(
            @PathVariable String groupId,
            @RequestBody SelectRescheduleRequest request,
            @AuthenticationPrincipal String email
    ) {
        assertGroupAccess(email, groupId);
        return ApiResponse.success(rescheduleGroupService.selectStrategy(groupId, request));
    }
}
