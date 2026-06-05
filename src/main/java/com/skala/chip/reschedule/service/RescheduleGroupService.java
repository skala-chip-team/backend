package com.skala.chip.reschedule.service;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.domain.ProcessQueue;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.domain.RescheduleSelection;
import com.skala.chip.reschedule.dto.RelatedRisk;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.RescheduleSelectionResponse;
import com.skala.chip.reschedule.dto.SelectRescheduleRequest;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import com.skala.chip.reschedule.repository.RescheduleSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 재조정 그룹 조회/선택 서비스.
 * 상세 페이지 조회와, 여러 전략 중 하나를 선택·확정하는 처리를 담당한다.
 */
@Service
@RequiredArgsConstructor
public class RescheduleGroupService {

    private static final String GROUP_STATUS_APPROVED = "approved";
    private static final String SELECTION_STATUS_APPLIED = "applied";

    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final DelayRiskRepository delayRiskRepository;
    private final RescheduleSelectionRepository rescheduleSelectionRepository;
    private final ProcessQueueRepository processQueueRepository;

    @Transactional(readOnly = true)
    public RescheduleGroupDetailResponse getGroupDetail(String groupId) {

        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 groupId입니다: " + groupId));

        ProcessStepOrder step = processStepOrderRepository.findById(group.getStepId())
                .orElse(null);

        List<String> memberRiskIds = group.getMemberRiskIds() != null
                ? group.getMemberRiskIds()
                : List.of();

        // member_risk_ids 의 순서를 유지하여 delay_risk 상세를 조립
        Map<String, DelayRisk> riskById = delayRiskRepository.findByRiskIdIn(memberRiskIds).stream()
                .collect(Collectors.toMap(
                        DelayRisk::getRiskId,
                        Function.identity(),
                        (a, b) -> a
                ));

        List<RelatedRisk> delayRisks = memberRiskIds.stream()
                .map(riskById::get)
                .filter(Objects::nonNull)
                .map(this::toRelatedRisk)
                .toList();

        return new RescheduleGroupDetailResponse(
                group.getGroupId(),
                group.getDistrictId(),
                group.getStepId(),
                step != null ? step.getProcessStep() : null,
                step != null ? step.getStepOrder() : null,
                group.getMaxRiskScore(),
                group.getGroupStatus(),
                group.getActedAt(),
                delayRisks,
                group.getRescheduleDetail()
        );
    }

    /**
     * 여러 후보 전략 중 하나를 선택·확정한다.
     * - reschedule_selection 에 그룹당 1건 저장 (재선택 시 덮어쓰기)
     * - 선택된 전략의 queue_reorder 를 process_queue 에 실제 반영
     * - reschedule_group.group_status 를 approved 로 변경
     */
    @Transactional
    public RescheduleSelectionResponse selectStrategy(String groupId, SelectRescheduleRequest request) {

        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 groupId입니다: " + groupId));

        if (request.strategy() == null || request.strategy().isBlank()) {
            throw new IllegalArgumentException("strategy 는 필수입니다.");
        }

        LocalDateTime now = LocalDateTime.now();

        // 그룹당 1건만 확정: 기존 선택이 있으면 덮어쓰기
        RescheduleSelection selection = rescheduleSelectionRepository.findByGroupId(groupId)
                .orElseGet(() -> RescheduleSelection.builder()
                        .selectionId("SEL-" + UUID.randomUUID())
                        .groupId(groupId)
                        .build());

        selection.setStrategy(request.strategy());
        selection.setRecommendation(request.recommendation());
        selection.setSelectedDetail(request.detail());
        selection.setStatus(SELECTION_STATUS_APPLIED);
        selection.setSelectedAt(now);
        rescheduleSelectionRepository.save(selection);

        // 선택된 전략의 queue_reorder 를 process_queue 에 실제 반영
        applyQueueReorder(request.detail());

        // 그룹 상태 확정
        group.setGroupStatus(GROUP_STATUS_APPROVED);
        group.setActedAt(now);
        rescheduleGroupRepository.save(group);

        return new RescheduleSelectionResponse(
                selection.getSelectionId(),
                groupId,
                selection.getStrategy(),
                selection.getStatus(),
                selection.getSelectedAt(),
                group.getGroupStatus()
        );
    }

    /**
     * 선택된 전략 상세의 queue_reorder 를 process_queue 에 반영한다.
     * queue_reorder[].queue_id 의 queue_position 을 new_queue_position 으로 갱신.
     */
    @SuppressWarnings("unchecked")
    private void applyQueueReorder(Map<String, Object> detail) {
        if (detail == null) {
            return;
        }
        Object queueReorder = detail.get("queue_reorder");
        if (!(queueReorder instanceof List<?> items)) {
            return;
        }

        for (Object item : items) {
            if (!(item instanceof Map<?, ?> entry)) {
                continue;
            }
            Object queueId = entry.get("queue_id");
            Object newPosition = entry.get("new_queue_position");
            if (!(queueId instanceof String qid) || !(newPosition instanceof Number pos)) {
                continue;
            }
            processQueueRepository.findById(qid).ifPresent(queue -> {
                queue.setQueuePosition(pos.intValue());
                processQueueRepository.save(queue);
            });
        }
    }

    private RelatedRisk toRelatedRisk(DelayRisk risk) {
        return new RelatedRisk(
                risk.getRiskId(),
                risk.getUnit() != null ? risk.getUnit().getUnitId() : null,
                risk.getRiskLevel(),
                risk.getRiskFactor(),
                risk.getRiskScore(),
                risk.getDelayProbability(),
                risk.getEstimatedDelayHr(),
                risk.getDetectionTime()
        );
    }
}
