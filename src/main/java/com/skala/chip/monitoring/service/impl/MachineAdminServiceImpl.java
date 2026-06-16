package com.skala.chip.monitoring.service.impl;

import com.skala.chip.exception.code.ErrorCode;
import com.skala.chip.exception.custom.BusinessException;
import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.MachineMaster;
import com.skala.chip.monitoring.domain.MachineStepMap;
import com.skala.chip.monitoring.domain.ProcessStepOrder;
import com.skala.chip.monitoring.dto.MachineAdminDTO;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.monitoring.repository.MachineStepMapRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.service.MachineAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MachineAdminServiceImpl implements MachineAdminService {

    private static final String MACHINE_ID_PREFIX = "MACHINE-";
    // 공정 매핑이 불가능한(사용 불가) 장비 상태
    private static final Set<String> UNUSABLE_STATUSES = Set.of("정지", "점검중");

    private final MachineRepository machineRepository;
    private final MachineStepMapRepository machineStepMapRepository;
    private final DistrictRepository districtRepository;
    private final ProcessStepOrderRepository processStepOrderRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MachineAdminDTO.MachineItem> getMachines(String districtId, String stepId) {
        boolean hasDistrict = StringUtils.hasText(districtId);
        List<MachineMaster> machines = hasDistrict
                ? machineRepository.findByDistrict_DistrictId(districtId)
                : machineRepository.findAll();

        List<MachineStepMap> maps = hasDistrict
                ? machineStepMapRepository.findByMachine_District_DistrictId(districtId)
                : machineStepMapRepository.findAll();
        Map<String, MachineStepMap> mapByMachine = maps.stream()
                .filter(m -> m.getMachine() != null && m.getMachine().getMachineId() != null)
                .collect(Collectors.toMap(
                        m -> m.getMachine().getMachineId(), Function.identity(), (a, b) -> a));

        return machines.stream()
                .map(m -> toItem(m, mapByMachine.get(m.getMachineId())))
                .filter(item -> !StringUtils.hasText(stepId) || stepId.equals(item.stepId()))
                .sorted(Comparator.comparing(MachineAdminDTO.MachineItem::machineId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MachineAdminDTO.ProcessStepOption> getProcessSteps() {
        // 공정 STEP 은 전역 4개(STEP_A~D). 구역 드롭다운용으로 (구역 × STEP) 조합을 제공한다.
        List<ProcessStepOrder> steps = processStepOrderRepository.findAll().stream()
                .sorted(Comparator.comparing(ProcessStepOrder::getStepOrder,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return districtRepository.findAll().stream()
                .flatMap(d -> steps.stream().map(s ->
                        new MachineAdminDTO.ProcessStepOption(s.getStepId(), s.getProcessStep(), d.getDistrictId())))
                .toList();
    }

    @Override
    @Transactional
    public MachineAdminDTO.MachineItem createMachine(MachineAdminDTO.UpsertRequest request) {
        validateRequired(request);
        DistrictMaster district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_NOT_FOUND));
        ProcessStepOrder step = resolveStep(request.stepId());
        // 공정 매핑 시 사용 불가 상태(정지/점검중) 장비는 등록 제한
        if (step != null) {
            assertMappable(request.machineStatus());
        }

        MachineMaster machine = MachineMaster.builder()
                .machineId(nextMachineId())
                .machineType(request.machineType())
                .district(district)
                .machineStatus(request.machineStatus())
                .build();
        machineRepository.save(machine);

        if (step != null) {
            machineStepMapRepository.save(MachineStepMap.builder()
                    .mapId(newMapId())
                    .machine(machine)
                    .step(step)
                    .build());
        }
        return toItem(machine, step);
    }

    @Override
    @Transactional
    public MachineAdminDTO.MachineItem updateMachine(String machineId, MachineAdminDTO.UpsertRequest request) {
        validateRequired(request);
        MachineMaster machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MACHINE_NOT_FOUND));
        DistrictMaster district = districtRepository.findById(request.districtId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DISTRICT_NOT_FOUND));
        ProcessStepOrder step = resolveStep(request.stepId());
        // 공정 매핑 시 사용 불가 상태(정지/점검중) 장비는 매핑 제한
        if (step != null) {
            assertMappable(request.machineStatus());
        }

        machine.setMachineType(request.machineType());
        machine.setDistrict(district);
        machine.setMachineStatus(request.machineStatus());
        machineRepository.save(machine);

        // 공정 매핑 갱신: 기존 매핑 제거 후 재생성 (MachineStepMap 은 불변이라 setter 없음)
        List<MachineStepMap> existing = machineStepMapRepository.findByMachine_MachineId(machineId);
        if (!existing.isEmpty()) {
            machineStepMapRepository.deleteAll(existing);
        }
        if (step != null) {
            machineStepMapRepository.save(MachineStepMap.builder()
                    .mapId(newMapId())
                    .machine(machine)
                    .step(step)
                    .build());
        }
        return toItem(machine, step);
    }

    @Override
    @Transactional
    public void deleteMachine(String machineId) {
        MachineMaster machine = machineRepository.findById(machineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MACHINE_NOT_FOUND));
        // 진행 중(active) 스케줄이 있으면 삭제 거절
        if (scheduleRepository.existsByMachine_MachineIdAndActiveTrue(machineId)) {
            throw new BusinessException(ErrorCode.MACHINE_HAS_ACTIVE_SCHEDULE);
        }
        machineStepMapRepository.deleteAll(machineStepMapRepository.findByMachine_MachineId(machineId));
        machineRepository.delete(machine);
    }

    // --- helpers ---

    /** 필수 입력값(장비 타입/구역) 검증. */
    private void validateRequired(MachineAdminDTO.UpsertRequest request) {
        if (request == null
                || !StringUtils.hasText(request.machineType())
                || !StringUtils.hasText(request.districtId())) {
            throw new BusinessException(ErrorCode.MACHINE_FIELD_REQUIRED);
        }
    }

    /** 사용 불가 상태(정지/점검중) 장비는 공정 매핑 불가. */
    private void assertMappable(String machineStatus) {
        if (machineStatus != null && UNUSABLE_STATUSES.contains(machineStatus)) {
            throw new BusinessException(ErrorCode.MACHINE_UNAVAILABLE_FOR_MAPPING);
        }
    }

    private ProcessStepOrder resolveStep(String stepId) {
        if (!StringUtils.hasText(stepId)) {
            return null;
        }
        return processStepOrderRepository.findById(stepId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROCESS_STEP_NOT_FOUND));
    }

    private MachineAdminDTO.MachineItem toItem(MachineMaster machine, MachineStepMap map) {
        ProcessStepOrder step = map != null ? map.getStep() : null;
        return toItem(machine, step);
    }

    private MachineAdminDTO.MachineItem toItem(MachineMaster machine, ProcessStepOrder step) {
        return new MachineAdminDTO.MachineItem(
                machine.getMachineId(),
                machine.getMachineType(),
                machine.getDistrict() != null ? machine.getDistrict().getDistrictId() : null,
                step != null ? step.getStepId() : null,
                step != null ? step.getProcessStep() : null,
                machine.getMachineStatus());
    }

    /** 기존 MACHINE-NN 중 최대 번호 + 1 로 새 ID 생성 (2자리 0패딩). */
    private String nextMachineId() {
        int max = machineRepository.findAll().stream()
                .map(MachineMaster::getMachineId)
                .filter(id -> id != null && id.startsWith(MACHINE_ID_PREFIX))
                .map(id -> id.substring(MACHINE_ID_PREFIX.length()))
                .filter(s -> s.matches("\\d+"))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return String.format("%s%02d", MACHINE_ID_PREFIX, max + 1);
    }

    private String newMapId() {
        return "MAP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
