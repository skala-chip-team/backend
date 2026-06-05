package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 위험 그룹핑 결과. step 그룹 목록(step_order 순)과 임계값/집계 메타를 담는다.
 * triggered=true 인 그룹이 이후 에이전트 호출 대상이 된다.
 */
public record RiskGroupingResponse(
        LocalDateTime detectionTime,   // 처리한 모델 실행 배치 시각
        double threshold,              // 적용된 임계값
        int totalRiskCount,            // 배치 내 원본 위험 건수
        int representativeCount,       // unit 대표 선정 후 건수
        int triggeredGroupCount,       // 임계값을 넘긴 step 그룹 수
        List<StepRiskGroup> groups
) {}
