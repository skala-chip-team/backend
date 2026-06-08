package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RescheduleGroupRepository extends JpaRepository<RescheduleGroup, String> {

    // 같은 (구역, step) 의 특정 상태 그룹 조회 (중복 방지용)
    Optional<RescheduleGroup> findByDistrictIdAndStepIdAndGroupStatus(
            String districtId, String stepId, String groupStatus);

    // 관리 목록: 구역별 조회
    List<RescheduleGroup> findByDistrictId(String districtId);

    // 특정 상태의 그룹 조회 (에이전트 호출 대상 선별 등)
    List<RescheduleGroup> findByGroupStatus(String groupStatus);

    // 자동 만료: acted_at 이 기준 시각보다 오래된 pending 그룹을 expired 로 일괄 전환
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RescheduleGroup g SET g.groupStatus = 'expired' "
            + "WHERE g.groupStatus = 'pending' AND g.actedAt < :threshold")
    int expirePendingOlderThan(@Param("threshold") LocalDateTime threshold);
}
