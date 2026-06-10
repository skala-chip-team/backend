package com.skala.chip.user.repository;

import com.skala.chip.user.domain.UserDistrictMap;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface UserDistrictMapRepository extends JpaRepository<UserDistrictMap, String> {

    List<UserDistrictMap> findByUserId(String userId);

    // 사용자 목록 조회 시 매핑을 한 번에 로딩 (N+1 방지)
    List<UserDistrictMap> findByUserIdIn(Collection<String> userIds);

    // 권한 재배정 시 기존 매핑 일괄 삭제
    void deleteByUserId(String userId);
}
