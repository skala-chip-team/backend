package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RescheduleGroupRepository extends JpaRepository<RescheduleGroup, String> {

    // 같은 (구역, step) 의 특정 상태 그룹 조회 (중복 방지용)
    Optional<RescheduleGroup> findByDistrictIdAndStepIdAndGroupStatus(
            String districtId, String stepId, String groupStatus);
}
