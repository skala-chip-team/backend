package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, String> {

    List<WorkStatus> findByMachine_MachineId(String machineId);

    // 특정 장비의 기준 시각 이후 작업(=금일 처리 유닛) 조회. 부하율 계산용.
    List<WorkStatus> findByMachine_MachineIdAndStartTimeGreaterThanEqual(String machineId, LocalDateTime startTime);

    List<WorkStatus> findBySchedule_ScheduleId(String scheduleId);

    List<WorkStatus> findByMachine_District_DistrictId(String districtId);

    List<WorkStatus> findByEndTimeIsNull();
}