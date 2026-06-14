package com.skala.chip.reschedule.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 재조정 오케스트레이션(모델 실행 → 그룹핑 → 에이전트 호출) 결과 요약.
 */
public record OrchestrationResponse(
        LocalDateTime snapTime,
        boolean predictCalled,
        String predictError,          // 모델 실행 실패 메시지 (성공 시 null)
        int triggeredGroupCount,      // 임계값 초과로 트리거된 그룹 수
        int detailGeneratedCount,     // 에이전트 호출이 성공해 재조정안이 채워진 그룹 수
        List<GroupAgentResult> groups
) {
    /** 그룹별 에이전트 호출 결과. */
    public record GroupAgentResult(
            String groupId,
            String riskId,
            boolean success,
            boolean notActionable,    // 현재 큐에 처리 가능한 위험이 없어 건너뜀(404/409). 서버 오류 아님.
            String error              // 실패 메시지 (성공 시 null)
    ) {}
}
