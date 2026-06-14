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
import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.domain.RescheduleSelection;
import com.skala.chip.reschedule.dto.AffectedUnit;
import com.skala.chip.reschedule.dto.RelatedRisk;
import com.skala.chip.reschedule.dto.RescheduleGroupDetailResponse;
import com.skala.chip.reschedule.dto.RescheduleGroupSummaryResponse;
import com.skala.chip.reschedule.dto.RescheduleOption;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
    private static final String GROUP_STATUS_EXPIRED = "expired";
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
                .orElseThrow(() -> new BusinessException(ErrorCode.RESCHEDULE_GROUP_NOT_FOUND));
        group.setRescheduleDetail(runResult);
        rescheduleGroupRepository.save(group);
    }

    /**
     * 그룹의 member_risk_ids 를 현재 tt_delay_risk 기준으로 재동기화하고 대표 risk_id 를 반환한다.
     *
     * 모델 재예측(/predict)으로 delay_risk 가 새 risk_id 로 재생성되면, 이전에 만들어진 그룹의
     * member_risk_ids 는 더 이상 존재하지 않는 risk_id 를 가리키게 된다(stale). 이 상태로
     * 에이전트(/run)를 호출하면 404(risk_id not found)가 난다.
     *
     * 그룹의 정체성은 (구역, step) 이고 risk_id 는 포인터일 뿐이므로, 생성 직전에 (구역, step) 의
     * 현재 위험을 unit 별 대표(최고 delay_probability)로 다시 뽑아 member_risk_ids 를 갱신한다.
     * 현재 위험이 없으면 기존 member_risk_ids 의 첫 항목을 그대로 사용한다(자가복구 불가).
     *
     * @return 갱신된 대표 risk_id. 현재 위험도 기존 위험도 없으면 null.
     */
    @Transactional
    public String resyncLiveRisks(String groupId) {
        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESCHEDULE_GROUP_NOT_FOUND));

        List<DelayRisk> live = delayRiskRepository
                .findByDistrict_DistrictIdAndStepId(group.getDistrictId(), group.getStepId());

        // (구역, step) 의 현재 위험이 없으면 기존 member_risk_ids 로 폴백
        if (live.isEmpty()) {
            List<String> existing = group.getMemberRiskIds();
            return (existing != null && !existing.isEmpty()) ? existing.get(0) : null;
        }

        // unit 별 대표(최고 delay_probability) → delay_probability 내림차순으로 정렬
        List<DelayRisk> representatives = live.stream()
                .filter(r -> r.getUnit() != null && r.getUnit().getUnitId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getUnit().getUnitId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .max(Comparator.comparing(
                                                DelayRisk::getDelayProbability,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                        .orElse(null))))
                .values().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        DelayRisk::getDelayProbability,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        if (representatives.isEmpty()) {
            List<String> existing = group.getMemberRiskIds();
            return (existing != null && !existing.isEmpty()) ? existing.get(0) : null;
        }

        List<String> memberRiskIds = representatives.stream()
                .map(DelayRisk::getRiskId)
                .toList();
        Double maxDelayProbability = representatives.stream()
                .map(DelayRisk::getDelayProbability)
                .filter(Objects::nonNull)
                .max(Double::compareTo)
                .orElse(group.getMaxRiskScore());

        group.setMemberRiskIds(memberRiskIds);
        group.setMaxRiskScore(maxDelayProbability);
        rescheduleGroupRepository.save(group);

        return memberRiskIds.get(0);
    }

    @Transactional(readOnly = true)
    public RescheduleGroupDetailResponse getGroupDetail(String groupId) {

        RescheduleGroup group = rescheduleGroupRepository.findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESCHEDULE_GROUP_NOT_FOUND));

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

        Map<String, Object> detail = group.getRescheduleDetail();

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
                extractRiskAnalysis(detail),
                extractBeforeSchedule(detail),
                buildOptions(detail)
        );
    }

    /** reschedule_detail.supervisor_payload.before_schedule (적용 전 스케줄) 추출. 없으면 null. */
    private Object extractBeforeSchedule(Map<String, Object> detail) {
        if (detail != null && detail.get("supervisor_payload") instanceof Map<?, ?> sp) {
            return ((Map<?, ?>) sp).get("before_schedule");
        }
        return null;
    }

    /** reschedule_detail 에서 에이전트 원인분석(risk_analysis) 서브객체를 꺼낸다. 없으면 null. */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractRiskAnalysis(Map<String, Object> detail) {
        if (detail != null && detail.get("risk_analysis") instanceof Map<?, ?> ra) {
            return (Map<String, Object>) ra;
        }
        return null;
    }

    /**
     * reschedule_detail 의 reschedule_options 와 decision_summaries 를 합쳐
     * 프론트가 바로 쓰는 전략별 카드(RescheduleOption) 목록으로 평탄화한다.
     * 에이전트 미호출/실패 시 빈 리스트를 반환한다.
     */
    @SuppressWarnings("unchecked")
    private List<RescheduleOption> buildOptions(Map<String, Object> detail) {
        List<?> rawOptions = extractOptions(detail);
        if (rawOptions == null || rawOptions.isEmpty()) {
            return List.of();
        }

        // 전략명 -> decision_summary (요약/추천)
        Map<String, Map<String, Object>> summaryByStrategy = new HashMap<>();
        if (detail.get("decision_summaries") instanceof List<?> summaries) {
            for (Object s : summaries) {
                if (s instanceof Map<?, ?> sm && sm.get("strategy") instanceof String strategy) {
                    summaryByStrategy.put(strategy, (Map<String, Object>) sm);
                }
            }
        }

        List<RescheduleOption> options = new ArrayList<>();
        for (Object o : rawOptions) {
            if (!(o instanceof Map<?, ?> om)) {
                continue;
            }
            Map<String, Object> opt = (Map<String, Object>) om;
            String strategy = asString(opt.get("strategy"));
            Map<String, Object> sim = opt.get("dispatch_simulation") instanceof Map<?, ?> sm
                    ? (Map<String, Object>) sm : Map.of();
            Map<String, Object> summary = summaryByStrategy.getOrDefault(strategy, Map.of());

            options.add(new RescheduleOption(
                    strategy,
                    asString(opt.get("analysis_status")),
                    asString(opt.get("fallback_reason")),
                    "recommend".equalsIgnoreCase(asString(summary.get("recommendation"))),
                    asString(summary.get("summary")),
                    opt.get("selected_yn") instanceof Boolean b && b,
                    asDouble(sim.get("estimated_delay_hr_after")),
                    asDouble(sim.get("avg_wait_time_min_after")),
                    asDouble(sim.get("avg_utilization_rate_after")),
                    asDouble(sim.get("max_wait_time_min_after")),
                    asInt(sim.get("deadline_violation_count")),
                    opt.get("after_schedule"),
                    opt.get("queue_reorder"),
                    buildMetricsComparison(summary.get("metrics_comparison")),
                    asString(summary.get("recommendation_reasoning")),
                    buildKeyPoints(summary.get("key_improvements")),
                    buildKeyPoints(summary.get("key_concerns")),
                    buildDetailedReport(summary.get("detailed_report")),
                    buildDeadlineImpact(summary.get("deadline_impact"))
            ));
        }
        return options;
    }

    /** key_improvements / key_concerns → KeyPoint 목록. 비었으면 빈 리스트. */
    private List<RescheduleOption.KeyPoint> buildKeyPoints(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<RescheduleOption.KeyPoint> points = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Map<?, ?> m) {
                points.add(new RescheduleOption.KeyPoint(
                        asString(m.get("description")),
                        asString(m.get("magnitude")),
                        asString(m.get("mitigation"))));
            }
        }
        return points;
    }

    /** detailed_report → DetailedReport. 없으면 null. */
    private RescheduleOption.DetailedReport buildDetailedReport(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) {
            return null;
        }
        return new RescheduleOption.DetailedReport(
                asString(m.get("executive_summary")),
                asString(m.get("risk_background")),
                asString(m.get("metric_analysis")),
                asString(m.get("tradeoffs")),
                asString(m.get("decision_basis")));
    }

    /** deadline_impact → DeadlineImpact. 없으면 null. */
    private RescheduleOption.DeadlineImpact buildDeadlineImpact(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) {
            return null;
        }
        return new RescheduleOption.DeadlineImpact(
                asInt(m.get("rescued_count")),
                asInt(m.get("still_at_risk_count")),
                asInt(m.get("newly_at_risk_count")),
                asInt(m.get("newly_violated_count")));
    }

    /**
     * decision_summaries[i].metrics_comparison 에서 적용 전/후 비교(생산량/지연/부하 차이)를 추출한다.
     * 구조: { throughput.completed_units, delay.{cumulative_delay_hr, avg_queue_wait_min,
     *        deadline_violation_count}, load.overall } 각각 { before, after, delta }.
     */
    @SuppressWarnings("unchecked")
    private RescheduleOption.MetricsComparison buildMetricsComparison(Object mcRaw) {
        if (!(mcRaw instanceof Map<?, ?> mc)) {
            return null;
        }
        Map<String, Object> mcMap = (Map<String, Object>) mc;
        Map<String, Object> throughput = asMap(mcMap.get("throughput"));
        Map<String, Object> delay = asMap(mcMap.get("delay"));
        Map<String, Object> load = asMap(mcMap.get("load"));

        RescheduleOption.Delta completedUnits = toDelta(throughput.get("completed_units"));
        RescheduleOption.Delta cumulativeDelayHr = toDelta(delay.get("cumulative_delay_hr"));
        RescheduleOption.Delta avgQueueWaitMin = toDelta(delay.get("avg_queue_wait_min"));
        RescheduleOption.Delta deadlineViolation = toDelta(delay.get("deadline_violation_count"));
        RescheduleOption.Delta overallLoad = toDelta(load.get("overall"));

        if (completedUnits == null && cumulativeDelayHr == null && avgQueueWaitMin == null
                && deadlineViolation == null && overallLoad == null) {
            return null;
        }
        return new RescheduleOption.MetricsComparison(
                completedUnits, cumulativeDelayHr, avgQueueWaitMin, deadlineViolation, overallLoad);
    }

    /** { before, after, delta } 묶음을 Delta 로. 셋 다 없으면 null. */
    private RescheduleOption.Delta toDelta(Object raw) {
        if (!(raw instanceof Map<?, ?> m)) {
            return null;
        }
        Double before = asDouble(m.get("before"));
        Double after = asDouble(m.get("after"));
        Double delta = asDouble(m.get("delta"));
        if (before == null && after == null && delta == null) {
            return null;
        }
        return new RescheduleOption.Delta(before, after, delta);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object v) {
        return v instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static String asString(Object v) {
        return v instanceof String s ? s : (v != null ? v.toString() : null);
    }

    private static Double asDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : null;
    }

    private static Integer asInt(Object v) {
        return v instanceof Number n ? n.intValue() : null;
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

        // 그룹 내 대표 위험(최고 등급) — 알림 배지용 등급과 원인을 함께 뽑는다.
        DelayRisk representative = memberRiskIds.stream()
                .map(riskById::get)
                .filter(Objects::nonNull)
                .filter(r -> r.getRiskLevel() != null)
                .max(Comparator.comparingInt(r -> riskLevelRank(r.getRiskLevel())))
                .orElse(null);
        String riskLevel = representative != null ? representative.getRiskLevel() : null;
        String riskFactor = representative != null ? representative.getRiskFactor() : null;

        return new RescheduleGroupSummaryResponse(
                group.getGroupId(),
                group.getDistrictId(),
                group.getStepId(),
                step != null ? step.getProcessStep() : null,
                group.getMaxRiskScore(),
                riskLevel,
                riskFactor,
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
                .orElseThrow(() -> new BusinessException(ErrorCode.RESCHEDULE_GROUP_NOT_FOUND));

        // 만료된 제안은 적용 불가 (생성 후 1시간 경과). 뒤늦은 스케줄 반영을 막는다.
        if (GROUP_STATUS_EXPIRED.equals(group.getGroupStatus())) {
            throw new BusinessException(ErrorCode.RESCHEDULE_EXPIRED);
        }

        if (request.strategy() == null || request.strategy().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Map<String, Object> detail = group.getRescheduleDetail();
        List<?> options = extractOptions(detail);
        if (options == null || options.isEmpty()) {
            throw new BusinessException(ErrorCode.RESCHEDULE_DETAIL_NOT_READY);
        }

        // 선택된 전략을 selected_yn=true 로 표시 (나머지는 false)
        Map<String, Object> selectedOption = markSelected(options, request.strategy());
        if (selectedOption == null) {
            throw new BusinessException(ErrorCode.RESCHEDULE_STRATEGY_NOT_FOUND);
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
