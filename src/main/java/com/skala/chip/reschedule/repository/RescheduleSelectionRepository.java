package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RescheduleSelectionRepository extends JpaRepository<RescheduleSelection, String> {

    // 그룹당 1건 (재선택 시 덮어쓰기용)
    Optional<RescheduleSelection> findByGroupId(String groupId);
}
