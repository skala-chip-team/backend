package com.skala.chip.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자-공정구역 매핑 (TT_USER_DISTRICT_MAP).
 * 한 사용자가 담당하는 구역(권한) 목록을 표현한다. (user_id, district_id) 는 유일하다.
 */
@Entity
@Table(name = "TT_USER_DISTRICT_MAP",
        uniqueConstraints = @UniqueConstraint(name = "uk_udm_user_district",
                columnNames = {"user_id", "district_id"}),
        indexes = @Index(name = "idx_udm_user", columnList = "user_id"))
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDistrictMap {

    @Id
    @Column(name = "map_id")
    private String mapId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "district_id", nullable = false)
    private String districtId;
}
