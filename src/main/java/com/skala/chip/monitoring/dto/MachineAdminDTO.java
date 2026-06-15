package com.skala.chip.monitoring.dto;

/**
 * 장비 설정(ADMIN) API DTO 모음.
 * 장비 목록/추가/수정, 공정 STEP 옵션.
 */
public class MachineAdminDTO {

    private MachineAdminDTO() {}

    /** 장비 목록/추가/수정 응답 1건. */
    public record MachineItem(
            String machineId,
            String machineType,   // TYPE_A ~ TYPE_D
            String districtId,
            String stepId,         // 매핑된 공정 STEP (없으면 null)
            String processStep,    // STEP_A ~ STEP_D (표시용, 없으면 null)
            String machineStatus   // 가동 / 대기 / 정지 / 점검중
    ) {}

    /** 장비 추가/수정 요청 바디. machineId 는 서버 생성. */
    public record UpsertRequest(
            String machineType,
            String districtId,
            String stepId,
            String machineStatus
    ) {}

    /** 공정 STEP 옵션 (장비-공정 매핑 드롭다운용). */
    public record ProcessStepOption(
            String stepId,
            String processStep,
            String districtId
    ) {}
}
