package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.MachineStatusEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MachineStatusEventRepository extends JpaRepository<MachineStatusEvent, String> {

    List<MachineStatusEvent> findByMachineIdAndStartTimeGreaterThanEqual(String machineId, LocalDateTime from);

    // 장애 지속시간(faultSince)용: 해당 상태(예: 정지)의 가장 최근 이벤트
    Optional<MachineStatusEvent> findTopByMachineIdAndStatusOrderByStartTimeDesc(String machineId, String status);
}
