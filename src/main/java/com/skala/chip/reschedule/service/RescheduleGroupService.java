package com.skala.chip.reschedule.service;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.dto.RelatedRisk;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.SelectRescheduleRequest;
import com.skala.chip.reschedule.dto.SelectRescheduleResponse;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 재조정 그룹 조회/선택 서비스.
 * 상세 페이지 조회와, 여러 전략 중 하나를 선택·확정하는 처리를 담당한다.
 * 선택 결과는 별도 테이블 없이 reschedule_group 안에서 관리한다.
 */
@Service
@RequiredArgsConstructor
public class RescheduleGroupService {

    private static final String GROUP_STATUS_APPROVED = "approved";

    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final DelayRiskRepository delayRiskRepository;
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
     * 여러 후보 전략 중 하나를 선택·확정한다 (별도 테이블 없이 reschedule_group 으로 처리).
     * - reschedule_detail.reschedule_options 에서 선택된 전략을 selected_yn=true 로 표시 (나머지 false)
     * - 선택된 전략의 queue_reorder 를 process_queue 에 실제 반영
     * - reschedule_group.group_status 를 approved 로 변경
     */
    @Transactional
    public SelectRescheduleResponse selectStrategy(String groupId, SelectRescheduleRequest request) {

        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 groupId입니다: " + groupId));

        if (request.strategy() == null || request.strategy().isBlank()) {
            throw new IllegalArgumentException("strategy 는 필수입니다.");
        }

        Map<String, Object> detail = group.getRescheduleDetail();
        List<?> options = (detail != null && detail.get("reschedule_options") instanceof List<?> list)
                ? list : null;
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("재조정안(reschedule_detail)이 아직 없습니다.");
        }

        // 선택된 전략을 selected_yn=true 로 표시 (나머지는 false)
        Map<String, Object> selectedOption = markSelected(options, request.strategy());
        if (selectedOption == null) {
            throw new IllegalArgumentException(
                    "해당 strategy 의 재조정안을 찾을 수 없습니다: " + request.strategy());
        }

        // 선택된 전략의 queue_reorder 를 process_queue 에 실제 반영
        applyQueueReorder(selectedOption);

        LocalDateTime now = LocalDateTime.now();
        group.setRescheduleDetail(detail);   // selected_yn 변경 반영
        group.setGroupStatus(GROUP_STATUS_APPROVED);
        group.setActedAt(now);
        rescheduleGroupRepository.save(group);

        return new SelectRescheduleResponse(
                groupId,
                request.strategy(),
                group.getGroupStatus(),
                now
        );
    }

    /**
     * reschedule_options 중 strategy 가 일치하는 옵션을 selected_yn=true, 나머지를 false 로 표시한다.
     * 선택된 옵션을 반환한다 (없으면 null).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> markSelected(List<?> options, String strategy) {
        Map<String, Object> selected = null;
        for (Object o : options) {
            if (!(o instanceof Map<?, ?> opt)) {
                continue;
            }
            Map<String, Object> option = (Map<String, Object>) opt;
            boolean isSelected = strategy.equals(option.get("strategy"));
            option.put("selected_yn", isSelected);
            if (isSelected) {
                selected = option;
            }
        }
        return selected;
    }

    /**
     * 선택된 전략의 queue_reorder 를 process_queue 에 반영한다.
     * queue_reorder[].queue_id 의 queue_position 을 new_queue_position 으로 갱신.
     */
    private void applyQueueReorder(Map<String, Object> option) {
        Object queueReorder = option.get("queue_reorder");
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
