package com.skala.chip.reschedule.dto;

import java.util.List;

/**
 * (구역, step) 기준으로 묶인 위험 그룹. 에이전트 호출 1회의 단위가 된다.
 * 같은 step 이라도 구역(district)이 다르면 별도 그룹으로 분리한다.
 * maxRiskScore 가 임계값 이상이면 triggered=true (에이전트 호출 대상).
 */
public record StepRiskGroup(
        String districtId,
        String stepId,
        String processStep,
        Integer stepOrder,
        double maxRiskScore,
        boolean triggered,
        List<RepresentativeRisk> risks
) {}
