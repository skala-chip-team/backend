package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RescheduleGroupRepository extends JpaRepository<RescheduleGroup, String> {

    // 같은 (구역, step) 의 특정 상태 그룹 조회 (중복 방지용)
    Optional<RescheduleGroup> findByDistrictIdAndStepIdAndGroupStatus(
            String districtId, String stepId, String groupStatus);

    // 관리 목록: 구역별 조회
    List<RescheduleGroup> findByDistrictId(String districtId);
}
