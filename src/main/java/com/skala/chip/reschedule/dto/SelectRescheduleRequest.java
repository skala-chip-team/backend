package com.skala.chip.reschedule.dto;

/**
 * 재조정 전략 선택 요청. 선택할 전략 이름만 전달한다.
 * (전략 상세는 reschedule_group.reschedule_detail 에 이미 있으므로 별도로 받지 않는다)
 */
public record SelectRescheduleRequest(
        String strategy
) {}
