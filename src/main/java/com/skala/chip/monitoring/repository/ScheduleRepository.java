package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.ScheduleMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleMaster, String> {

    List<ScheduleMaster> findByActiveTrue();

    List<ScheduleMaster> findByMachine_MachineId(String machineId);

    List<ScheduleMaster> findByUnit_UnitId(String unitId);

    List<ScheduleMaster> findByMachine_District_DistrictId(String districtId);

    List<ScheduleMaster> findByMachine_MachineIdAndStatusAndActiveTrue(String machineId, String status);

    // 장비 삭제 가드: 진행 중(active) 스케줄이 있으면 삭제 거절.
    boolean existsByMachine_MachineIdAndActiveTrue(String machineId);
}