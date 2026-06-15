package com.skala.chip.monitoring.controller;

import com.skala.chip.common.ApiResponse;
import com.skala.chip.monitoring.dto.MachineAdminDTO;
import com.skala.chip.monitoring.service.MachineAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "MachineAdmin", description = "장비 설정 API (ADMIN)")
@SecurityRequirement(name = "BearerAuth")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MachineAdminController {

    private final MachineAdminService machineAdminService;

    @Operation(summary = "장비 목록 조회", description = "구역(districtId)/공정STEP(stepId)으로 필터링. 둘 다 선택값.")
    @GetMapping("/machines")
    public ResponseEntity<ApiResponse<List<MachineAdminDTO.MachineItem>>> getMachines(
            @RequestParam(required = false) String districtId,
            @RequestParam(required = false) String stepId) {
        return ResponseEntity.ok(ApiResponse.success(machineAdminService.getMachines(districtId, stepId)));
    }

    @Operation(summary = "공정 STEP 옵션 조회", description = "장비-공정 매핑 드롭다운용 (구역 × STEP).")
    @GetMapping("/process-steps")
    public ResponseEntity<ApiResponse<List<MachineAdminDTO.ProcessStepOption>>> getProcessSteps() {
        return ResponseEntity.ok(ApiResponse.success(machineAdminService.getProcessSteps()));
    }

    @Operation(summary = "장비 추가", description = "machineId 는 서버에서 MACHINE-NN 형식으로 생성.")
    @PostMapping("/machines")
    public ResponseEntity<ApiResponse<MachineAdminDTO.MachineItem>> createMachine(
            @RequestBody MachineAdminDTO.UpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(machineAdminService.createMachine(request)));
    }

    @Operation(summary = "장비 수정")
    @PutMapping("/machines/{machineId}")
    public ResponseEntity<ApiResponse<MachineAdminDTO.MachineItem>> updateMachine(
            @PathVariable String machineId,
            @RequestBody MachineAdminDTO.UpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.success(machineAdminService.updateMachine(machineId, request)));
    }

    @Operation(summary = "장비 삭제", description = "진행 중(active) 스케줄이 있으면 409 로 거절.")
    @DeleteMapping("/machines/{machineId}")
    public ResponseEntity<ApiResponse<Void>> deleteMachine(@PathVariable String machineId) {
        machineAdminService.deleteMachine(machineId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
