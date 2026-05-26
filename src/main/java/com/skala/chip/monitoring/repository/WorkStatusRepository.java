package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, String> {

    List<WorkStatus> findByMachine_MachineId(String machineId);

    List<WorkStatus> findBySchedule_ScheduleId(String scheduleId);

    List<WorkStatus> findByMachine_District_DistrictId(String districtId);

    List<WorkStatus> findByEndTimeIsNull();
}