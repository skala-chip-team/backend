package com.skala.chip.reschedule.service;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.domain.MachineMaster;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.domain.ScheduleMaster;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.domain.RescheduleSelection;
import com.skala.chip.reschedule.dto.AffectedUnit;
import com.skala.chip.reschedule.dto.RelatedRisk;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupSummaryResponse;
import com.skala.chip.reschedule.dto.RescheduleSelectionResponse;
import com.skala.chip.reschedule.dto.SelectRescheduleRequest;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import com.skala.chip.reschedule.repository.RescheduleSelectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 재조정 그룹 조회/선택 서비스.
 * 상세 페이지 조회와, 여러 전략 중 하나를 선택·확정하는 처리를 담당한다.
 * 선택 결과는 별도 테이블 없이 reschedule_group 안에서 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RescheduleGroupService {

    private static final String GROUP_STATUS_APPROVED = "approved";
    private static final String GROUP_STATUS_PENDING = "pending";
    private static final String STATUS_FILTER_ACTIVE = "active";   // 진행중 = pending + approved
    private static final String SELECTION_STATUS_APPLIED = "applied";

    private final RescheduleGroupRepository rescheduleGroupRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final DelayRiskRepository delayRiskRepository;
    private final ProcessQueueRepository processQueueRepository;
    private final RescheduleSelectionRepository rescheduleSelectionRepository;
    private final ScheduleRepository scheduleRepository;
    private final MachineRepository machineRepository;

    /**
     * 에이전트(/run) 결과를 그룹의 reschedule_detail 에 저장한다.
     * RunResponse 전체(risk_analysis, reschedule_result, decision_summaries ...)를 통째로 보관해
     * 상세 페이지에서 근거/요약까지 노출할 수 있게 한다.
     * 상태는 pending 그대로 유지(승인 전 단계).
     */
    @Transactional
    public void applyAgentResult(String groupId, Map<String, Object> runResult) {
        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 groupId입니다: " + groupId));
        group.setRescheduleDetail(runResult);
        rescheduleGroupRepository.save(group);
    }

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
     * 재조정안 관리(목록) 조회.
     * @param districtId 구역 필터 (null/blank = 전체)
     * @param status     상태 필터 (active=진행중(pending+approved) / expired / 특정 상태 / null=전체)
     */
    @Transactional(readOnly = true)
    public List<RescheduleGroupSummaryResponse> getGroups(String districtId, String status) {

        List<RescheduleGroup> groups = (districtId != null && !districtId.isBlank())
                ? rescheduleGroupRepository.findByDistrictId(districtId)
                : rescheduleGroupRepository.findAll();

        groups = filterByStatus(groups, status);

        // 공정 단계명 맵
        Map<String, ProcessStepOrder> stepMap = processStepOrderRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ProcessStepOrder::getStepId, Function.identity(), (a, b) -> a));

        // 모든 그룹의 member_risk_ids 를 모아 한 번에 delay_risk 조회 (N+1 방지)
        List<String> allRiskIds = groups.stream()
                .map(RescheduleGroup::getMemberRiskIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .distinct()
                .toList();
        Map<String, DelayRisk> riskById = allRiskIds.isEmpty()
                ? Map.of()
                : delayRiskRepository.findByRiskIdIn(allRiskIds).stream()
                        .collect(Collectors.toMap(
                                DelayRisk::getRiskId, Function.identity(), (a, b) -> a));

        return groups.stream()
                .map(g -> toSummary(g, stepMap, riskById))
                // 생성 시각 최신순
                .sorted(Comparator.comparing(
                        RescheduleGroupSummaryResponse::createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private List<RescheduleGroup> filterByStatus(List<RescheduleGroup> groups, String status) {
        if (status == null || status.isBlank()) {
            return groups;
        }
        if (STATUS_FILTER_ACTIVE.equals(status)) {
            return groups.stream()
                    .filter(g -> GROUP_STATUS_PENDING.equals(g.getGroupStatus())
                            || GROUP_STATUS_APPROVED.equals(g.getGroupStatus()))
                    .toList();
        }
        return groups.stream()
                .filter(g -> status.equals(g.getGroupStatus()))
                .toList();
    }

    /** 위험 등급 심각도 순위 (그룹 최고등급 산출용). Critical > High > Medium > Low. */
    private static int riskLevelRank(String level) {
        if (level == null) {
            return 0;
        }
        return switch (level.toLowerCase()) {
            case "critical" -> 4;
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private RescheduleGroupSummaryResponse toSummary(
            RescheduleGroup group,
            Map<String, ProcessStepOrder> stepMap,
            Map<String, DelayRisk> riskById
    ) {
        ProcessStepOrder step = stepMap.get(group.getStepId());
        List<String> memberRiskIds = group.getMemberRiskIds() != null
                ? group.getMemberRiskIds() : List.of();

        List<AffectedUnit> affectedUnits = memberRiskIds.stream()
                .map(riskById::get)
                .filter(Objects::nonNull)
                .map(r -> new AffectedUnit(
                        r.getUnit() != null ? r.getUnit().getUnitId() : null,
                        r.getEstimatedDelayHr()))
                .toList();

        // 그룹 내 최고 위험 등급 (알림 배지용)
        String riskLevel = memberRiskIds.stream()
                .map(riskById::get)
                .filter(Objects::nonNull)
                .map(DelayRisk::getRiskLevel)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(RescheduleGroupService::riskLevelRank))
                .orElse(null);

        return new RescheduleGroupSummaryResponse(
                group.getGroupId(),
                group.getDistrictId(),
                group.getStepId(),
                step != null ? step.getProcessStep() : null,
                group.getMaxRiskScore(),
                riskLevel,
                group.getGroupStatus(),
                group.getActedAt(),
                affectedUnits
        );
    }

    /**
     * 여러 후보 전략 중 하나를 선택·확정한다.
     * - reschedule_detail.reschedule_options 에서 선택된 전략을 selected_yn=true 로 표시 (나머지 false)
     * - 선택 결과를 reschedule_selection 에 저장 (그룹당 1건, 재선택 시 덮어쓰기)
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

        Map<String, Object> detail = group.getRescheduleDetail();
        List<?> options = extractOptions(detail);
        if (options == null || options.isEmpty()) {
            throw new IllegalArgumentException("재조정안(reschedule_detail)이 아직 없습니다.");
        }

        // 선택된 전략을 selected_yn=true 로 표시 (나머지는 false)
        Map<String, Object> selectedOption = markSelected(options, request.strategy());
        if (selectedOption == null) {
            throw new IllegalArgumentException(
                    "해당 strategy 의 재조정안을 찾을 수 없습니다: " + request.strategy());
        }

        LocalDateTime now = LocalDateTime.now();

        // reschedule_selection 에 저장 (그룹당 1건, 재선택 시 덮어쓰기)
        RescheduleSelection selection = rescheduleSelectionRepository.findByGroupId(groupId)
                .orElseGet(() -> RescheduleSelection.builder()
                        .selectionId("SEL-" + UUID.randomUUID())
                        .groupId(groupId)
                        .build());
        selection.setStrategy(request.strategy());
        selection.setSelectedDetail(selectedOption);
        selection.setStatus(SELECTION_STATUS_APPLIED);
        selection.setSelectedAt(now);
        rescheduleSelectionRepository.save(selection);

        // 선택된 전략의 queue_reorder 를 process_queue 에 실제 반영
        applyQueueReorder(selectedOption);

        // 선택된 전략의 after_schedule 을 schedule_master 에 실제 반영
        applyAfterSchedule(selectedOption);

        // reschedule_group 갱신 (selected_yn 반영 + 상태 확정)
        // acted_at(생성 시각)은 보존한다. 선택 시각은 reschedule_selection.selected_at 에 기록됨.
        group.setRescheduleDetail(detail);
        group.setGroupStatus(GROUP_STATUS_APPROVED);
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

    /**
     * reschedule_detail 에서 옵션 목록을 꺼낸다.
     * 에이전트(RunResponse) 결과는 reschedule_result.reschedule_options 에 있고,
     * 과거 형식(최상위 reschedule_options)도 호환 처리한다.
     */
    private List<?> extractOptions(Map<String, Object> detail) {
        if (detail == null) {
            return null;
        }
        if (detail.get("reschedule_result") instanceof Map<?, ?> result
                && result.get("reschedule_options") instanceof List<?> nested) {
            return nested;
        }
        if (detail.get("reschedule_options") instanceof List<?> flat) {
            return flat;
        }
        return null;
    }

    /**
     * 선택된 전략의 after_schedule 을 schedule_master 에 반영한다.
     *
     * after_schedule 형식(Schedule): { "units": [ { "unit_id", "steps": [
     *   { "step_id", "start", "finish", "machine_id" } ] } ] }
     *
     * (unit_id, step_id) 로 schedule_master 행을 찾아 estimated_start 와 machine 을 갱신한다.
     * 같은 (unit, step) 행이 여럿이면 active=true 행을 우선한다.
     */
    private void applyAfterSchedule(Map<String, Object> option) {
        Object afterSchedule = option.get("after_schedule");
        if (!(afterSchedule instanceof Map<?, ?> schedule)) {
            return;
        }
        if (!(schedule.get("units") instanceof List<?> units)) {
            return;
        }

        int updated = 0;
        for (Object u : units) {
            if (!(u instanceof Map<?, ?> unit) || !(unit.get("unit_id") instanceof String unitId)) {
                continue;
            }
            if (!(unit.get("steps") instanceof List<?> steps)) {
                continue;
            }

            // 해당 unit 의 schedule 행을 step_id 별로 (active 우선) 인덱싱
            Map<String, ScheduleMaster> byStep = scheduleRepository.findByUnit_UnitId(unitId).stream()
                    .filter(s -> s.getStepId() != null)
                    .collect(Collectors.toMap(
                            ScheduleMaster::getStepId,
                            Function.identity(),
                            (a, b) -> Boolean.TRUE.equals(a.getActive()) ? a : b));

            for (Object st : steps) {
                if (!(st instanceof Map<?, ?> step) || !(step.get("step_id") instanceof String stepId)) {
                    continue;
                }
                ScheduleMaster row = byStep.get(stepId);
                if (row == null) {
                    continue;
                }

                LocalDateTime start = parseDateTime(step.get("start"));
                if (start != null) {
                    row.setEstimatedStart(start);
                }
                if (step.get("machine_id") instanceof String machineId && !machineId.isBlank()) {
                    MachineMaster machine = machineRepository.findById(machineId).orElse(null);
                    if (machine != null) {
                        row.setMachine(machine);
                    }
                }
                scheduleRepository.save(row);
                updated++;
            }
        }
        log.info("after_schedule 적용: schedule_master {}건 갱신", updated);
    }

    /** ISO 날짜시간 문자열(오프셋 유무 모두)을 LocalDateTime 으로 파싱한다. */
    private LocalDateTime parseDateTime(Object value) {
        if (!(value instanceof String s) || s.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(s).toLocalDateTime();
            } catch (DateTimeParseException e) {
                log.warn("after_schedule start 시각 파싱 실패: {}", s);
                return null;
            }
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
