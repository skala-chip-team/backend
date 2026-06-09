package com.skala.chip.reschedule.service;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.reschedule.client.AiAgentClient;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.dto.OrchestrationResponse;
import com.skala.chip.reschedule.dto.OrchestrationResponse.GroupAgentResult;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 재조정 전체 흐름 오케스트레이터.
 *
 *   ① 트리거 → 모델 실행(/predict): snap_time 기준 지연 예측 → delay_risk 갱신
 *   ② 그룹핑/탐지(RiskGroupingService): delay_risk 를 (구역, step) 으로 묶고 임계값 게이트
 *   ③ 임계값 초과(triggered) 그룹마다 에이전트 호출(/run) → reschedule_detail 채움
 *   ④ 이후 운영자가 승인하면 RescheduleGroupService.selectStrategy 가 실제 스케줄에 반영
 *
 * 에이전트 호출(/run)은 LLM 이라 느리고 그룹마다 독립적이므로, 긴 트랜잭션을 피하기 위해
 * 각 호출을 짧은 트랜잭션(applyAgentResult)으로 분리한다. 한 그룹이 실패해도 나머지는 진행한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RescheduleOrchestrationService {

    private static final String GROUP_STATUS_PENDING = "pending";

    private final AiAgentClient aiAgentClient;
    private final RiskGroupingService riskGroupingService;
    private final RescheduleGroupService rescheduleGroupService;
    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final DelayRiskRepository delayRiskRepository;

    /**
     * 전체 흐름 실행. snapTime 미지정 시 최신 delay_risk 시각을 사용한다.
     */
    public OrchestrationResponse orchestrate(LocalDateTime snapTime) {

        LocalDateTime effectiveSnapTime = (snapTime != null)
                ? snapTime
                : delayRiskRepository.findTopByOrderByDetectionTimeDesc()
                        .map(DelayRisk::getDetectionTime)
                        .orElse(null);

        // ① 모델 실행 (실패는 비치명적 — 기존 delay_risk 로 그룹핑 진행)
        boolean predictCalled = false;
        String predictError = null;
        if (effectiveSnapTime != null) {
            try {
                aiAgentClient.predict(effectiveSnapTime);
                predictCalled = true;
            } catch (RuntimeException e) {
                predictError = e.getMessage();
                log.warn("모델 실행(/predict) 실패, 기존 delay_risk 로 진행: {}", e.getMessage());
            }
        }

        // ② 그룹핑/탐지 (자체 트랜잭션에서 pending 그룹 커밋)
        riskGroupingService.groupRisks(effectiveSnapTime);

        // ③ 재조정안 없는 pending 그룹마다 에이전트 호출
        List<RescheduleGroup> targets = rescheduleGroupRepository
                .findByGroupStatus(GROUP_STATUS_PENDING).stream()
                .filter(g -> g.getRescheduleDetail() == null)
                .toList();

        List<GroupAgentResult> results = new ArrayList<>();
        int generated = 0;
        for (RescheduleGroup group : targets) {
            GroupAgentResult result = generateInternal(group);
            results.add(result);
            if (result.success()) {
                generated++;
            }
        }

        return new OrchestrationResponse(
                effectiveSnapTime,
                predictCalled,
                predictError,
                targets.size(),
                generated,
                results
        );
    }

    /**
     * 자동 트리거(스케줄러)용: 모델 실행(predict) 없이
     * 최신 delay_risk 를 그룹핑하고, 재조정안 없는 pending(트리거된 High/Critical) 그룹마다
     * 에이전트를 호출해 reschedule_detail 을 채운다. 멱등(이미 detail 있는 그룹은 스킵).
     *
     * @return 이번 실행에서 에이전트 호출이 성공해 새로 생성된 재조정안 수
     */
    public int triggerAndGenerate() {
        // 최신 배치 그룹핑 → triggered(High/Critical) 그룹을 pending 으로 저장 (자체 트랜잭션)
        riskGroupingService.groupRisks(null);

        List<RescheduleGroup> targets = rescheduleGroupRepository
                .findByGroupStatus(GROUP_STATUS_PENDING).stream()
                .filter(g -> g.getRescheduleDetail() == null)
                .toList();

        int generated = 0;
        for (RescheduleGroup group : targets) {
            if (generateInternal(group).success()) {
                generated++;
            }
        }
        return generated;
    }

    /**
     * 단일 그룹에 대해 에이전트를 (재)호출해 재조정안을 채운다. (수동 재생성용)
     */
    public RescheduleGroupDetailResponse generateForGroup(String groupId) {
        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 groupId입니다: " + groupId));
        GroupAgentResult result = generateInternal(group);
        if (!result.success()) {
            throw new AiAgentClient.AiAgentException(
                    "재조정안 생성 실패: " + result.error(), null);
        }
        return rescheduleGroupService.getGroupDetail(groupId);
    }

    /**
     * 그룹의 대표 risk_id 로 에이전트를 호출하고 결과를 저장한다.
     * HTTP 호출은 트랜잭션 밖에서 수행하고, 저장만 짧은 트랜잭션으로 처리한다.
     */
    private GroupAgentResult generateInternal(RescheduleGroup group) {
        String riskId = representativeRiskId(group);
        if (riskId == null) {
            return new GroupAgentResult(group.getGroupId(), null, false,
                    "대표 risk_id 가 없습니다 (member_risk_ids 비어있음)");
        }
        try {
            var runResult = aiAgentClient.run(riskId);
            rescheduleGroupService.applyAgentResult(group.getGroupId(), runResult);
            return new GroupAgentResult(group.getGroupId(), riskId, true, null);
        } catch (RuntimeException e) {
            log.warn("그룹 {} 에이전트 호출 실패 (risk_id={}): {}",
                    group.getGroupId(), riskId, e.getMessage());
            return new GroupAgentResult(group.getGroupId(), riskId, false, e.getMessage());
        }
    }

    /** member_risk_ids 의 첫 번째(=delay_probability 최고) 위험을 대표로 사용한다. */
    private String representativeRiskId(RescheduleGroup group) {
        List<String> ids = group.getMemberRiskIds();
        return (ids != null && !ids.isEmpty()) ? ids.get(0) : null;
    }
}
