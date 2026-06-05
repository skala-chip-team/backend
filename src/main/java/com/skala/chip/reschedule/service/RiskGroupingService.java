package com.skala.chip.reschedule.service;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.reschedule.domain.RescheduleGroup;
import com.skala.chip.reschedule.dto.RepresentativeRisk;
import com.skala.chip.reschedule.dto.RiskGroupingResponse;
import com.skala.chip.reschedule.dto.StepRiskGroup;
import com.skala.chip.reschedule.repository.RescheduleGroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 모델 실행 배치(delay_risk)를 에이전트 호출 단위로 그룹핑한다.
 *
 * 중복 재조정안 방지를 위해 다음 순서로 묶는다.
 *  ① 같은 배치(detection_time)의 delay_risk 를 unit_id 별로 묶기
 *  ② 각 unit 에서 가장 앞선 위험 step(min step_order)의 delay_risk 만 대표로 남기기
 *  ③ 남은 대표들을 (구역, step) 기준으로 다시 묶기
 *  ④ 그룹별 max(delay_probability) 가 임계값 이상이면 에이전트 호출 대상(triggered)
 *
 * triggered 그룹은 reschedule_group 테이블에 pending 상태로 저장한다.
 */
@Service
@RequiredArgsConstructor
public class RiskGroupingService {

    private static final String STATUS_PENDING = "pending";

    // 위험 그룹핑 임계값 (하드코딩, 사용자 변경 대상 아님)
    private static final double RISK_THRESHOLD = 0.7;

    private final DelayRiskRepository delayRiskRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final RescheduleGroupRepository rescheduleGroupRepository;

    @Transactional
    public RiskGroupingResponse groupRisks(LocalDateTime detectionTime) {

        double threshold = RISK_THRESHOLD;

        // 기준 시각 결정 (미지정 시 최신 위험 시각 사용)
        LocalDateTime baseTime = (detectionTime != null)
                ? detectionTime
                : delayRiskRepository.findTopByOrderByDetectionTimeDesc()
                        .map(DelayRisk::getDetectionTime)
                        .orElse(null);

        if (baseTime == null) {
            return new RiskGroupingResponse(null, threshold, 0, 0, 0, List.of());
        }

        // 시(時) 단위 배치로 묶는다: [hourStart, hourStart+1h)
        // detection_time 이 60분 단위로 떨어지지 않아도 같은 시간대를 한 배치로 처리한다.
        LocalDateTime hourStart = baseTime.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime hourEnd = hourStart.plusHours(1);

        List<DelayRisk> risks = delayRiskRepository
                .findByDetectionTimeGreaterThanEqualAndDetectionTimeLessThan(hourStart, hourEnd);

        // step_id -> 공정 단계 정의(step_order 등)
        Map<String, ProcessStepOrder> stepMap = processStepOrderRepository.findAll().stream()
                .collect(Collectors.toMap(
                        ProcessStepOrder::getStepId,
                        Function.identity(),
                        (a, b) -> a
                ));

        // ① unit_id 별 그룹 → ② 가장 앞선 위험 step(min step_order)의 delay_risk 만 대표로
        Map<String, DelayRisk> representativeByUnit = risks.stream()
                .filter(r -> r.getUnit() != null && r.getStepId() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getUnit().getUnitId(),
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .min(Comparator.comparing(
                                                r -> stepOrderOf(stepMap, r.getStepId()),
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                        .orElse(null)
                        )
                ));

        List<DelayRisk> representatives = representativeByUnit.values().stream()
                .filter(Objects::nonNull)
                .toList();

        // ③ (구역, step) 기준 재그룹 → ④ max(risk_score) 게이트
        // 같은 step 이라도 구역이 다르면 별도 그룹으로 분리한다.
        Map<GroupKey, List<DelayRisk>> byDistrictStep = representatives.stream()
                .collect(Collectors.groupingBy(r -> new GroupKey(
                        r.getDistrict() != null ? r.getDistrict().getDistrictId() : null,
                        r.getStepId()
                )));

        List<StepRiskGroup> groups = byDistrictStep.entrySet().stream()
                .map(entry -> toStepGroup(entry.getKey(), entry.getValue(), stepMap, threshold))
                .sorted(Comparator
                        .comparing(StepRiskGroup::districtId,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(StepRiskGroup::stepOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        // triggered 그룹을 reschedule_group 에 pending 상태로 저장 (구역+step 중복 방지)
        List<RescheduleGroup> persisted = groups.stream()
                .filter(StepRiskGroup::triggered)
                .filter(g -> g.districtId() != null)   // district_id 는 NOT NULL
                .map(this::upsertRescheduleGroup)
                .toList();
        rescheduleGroupRepository.saveAll(persisted);

        int triggeredGroupCount = (int) groups.stream()
                .filter(StepRiskGroup::triggered)
                .count();

        return new RiskGroupingResponse(
                hourStart,
                threshold,
                risks.size(),
                representatives.size(),
                triggeredGroupCount,
                groups
        );
    }

    private StepRiskGroup toStepGroup(
            GroupKey key,
            List<DelayRisk> groupRisks,
            Map<String, ProcessStepOrder> stepMap,
            double threshold
    ) {
        ProcessStepOrder stepDef = stepMap.get(key.stepId());

        // 게이트/저장 기준: delay_probability 의 최대값 (0.0~1.0 범위)
        double maxRiskScore = groupRisks.stream()
                .map(DelayRisk::getDelayProbability)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .max()
                .orElse(0.0);

        boolean triggered = maxRiskScore >= threshold;

        // delay_probability 높은 순으로 정렬
        List<RepresentativeRisk> risks = groupRisks.stream()
                .sorted(Comparator.comparing(
                        DelayRisk::getDelayProbability,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRepresentative)
                .toList();

        return new StepRiskGroup(
                key.districtId(),
                key.stepId(),
                stepDef != null ? stepDef.getProcessStep() : null,
                stepDef != null ? stepDef.getStepOrder() : null,
                maxRiskScore,
                triggered,
                risks
        );
    }

    // triggered step 그룹 → reschedule_group (pending) 저장/갱신.
    // 같은 (구역, step) 의 pending 그룹이 있으면 새로 만들지 않고 갱신한다 (중복 방지).
    private RescheduleGroup upsertRescheduleGroup(StepRiskGroup group) {
        List<String> memberRiskIds = group.risks().stream()
                .map(RepresentativeRisk::riskId)
                .toList();
        LocalDateTime now = LocalDateTime.now();

        RescheduleGroup existing = rescheduleGroupRepository
                .findByDistrictIdAndStepIdAndGroupStatus(
                        group.districtId(), group.stepId(), STATUS_PENDING)
                .orElse(null);

        if (existing != null) {
            existing.setMemberRiskIds(memberRiskIds);
            existing.setMaxRiskScore(group.maxRiskScore());
            existing.setActedAt(now);
            return existing;
        }

        return RescheduleGroup.builder()
                .groupId("GRP-" + UUID.randomUUID())
                .districtId(group.districtId())
                .stepId(group.stepId())
                .memberRiskIds(memberRiskIds)
                .maxRiskScore(group.maxRiskScore())
                .groupStatus(STATUS_PENDING)
                .actedAt(now)
                .build();
    }

    // (구역, step) 복합 그룹 키
    private record GroupKey(String districtId, String stepId) {}

    private Integer stepOrderOf(Map<String, ProcessStepOrder> stepMap, String stepId) {
        ProcessStepOrder stepDef = stepMap.get(stepId);
        return stepDef != null ? stepDef.getStepOrder() : null;
    }

    private RepresentativeRisk toRepresentative(DelayRisk risk) {
        return new RepresentativeRisk(
                risk.getRiskId(),
                risk.getUnit() != null ? risk.getUnit().getUnitId() : null,
                risk.getStepId(),
                risk.getDistrict() != null ? risk.getDistrict().getDistrictId() : null,
                risk.getRiskScore(),
                risk.getRiskLevel(),
                risk.getRiskFactor(),
                risk.getEstimatedDelayHr(),
                risk.getDelayProbability(),
                risk.getDetectionTime()
        );
    }
}
