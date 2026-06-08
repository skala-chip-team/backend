package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.MachineStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MachineStatusEventRepository extends JpaRepository<MachineStatusEvent, String> {

    List<MachineStatusEvent> findByMachineIdAndStartTimeGreaterThanEqual(String machineId, LocalDateTime from);
}
