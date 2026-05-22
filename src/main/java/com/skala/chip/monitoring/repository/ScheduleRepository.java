package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.ScheduleMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<ScheduleMaster, String> {

    List<ScheduleMaster> findByActiveTrue();

    List<ScheduleMaster> findByMachine_MachineId(String machineId);

    List<ScheduleMaster> findByUnit_UnitId(String unitId);
}