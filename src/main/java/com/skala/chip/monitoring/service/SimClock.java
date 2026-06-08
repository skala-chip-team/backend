package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.repository.DistrictRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 시뮬레이션 기준 "현재 시각" 제공.
 *
 * 시뮬레이션 엔진(ai_agent)은 배속으로 돌며 DB에 시각을 기록하는데, 그 값은 실제 벽시계가 아니라
 * 시뮬레이션 달력(BASE_DATE = 2025-05-01 기준) 좌표다. 따라서 모니터링에서 "오늘/지금"을
 * {@link LocalDateTime#now()}(실제 시각)로 잡으면 시뮬레이션 데이터와 어긋나 결과가 항상 비게 된다.
 *
 * {@code district_status.created_at} 은 시뮬레이션이 60 sim-min 마다 남기는 heartbeat 이므로,
 * 그 최신값을 시뮬레이션 기준 "지금"으로 사용한다. 시뮬레이션 데이터가 아직 없으면(테이블 미생성
 * 또는 빈 상태) 실제 시각으로 폴백한다.
 */
@Component
@RequiredArgsConstructor
public class SimClock {

    private final DistrictRepository districtRepository;

    /**
     * 시뮬레이션 기준 현재 시각. 데이터가 없으면 실제 시각으로 폴백한다.
     *
     * <p>{@code district_status} 는 백엔드 엔티티가 없어 네이티브 쿼리로 조회한다. 테이블이 아직
     * 생성되지 않은 환경(시뮬레이션 미실행)에서 발생할 수 있는 DB 오류가 호출자의 트랜잭션을
     * 오염시키지 않도록 {@code REQUIRES_NEW} 로 분리한다.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public LocalDateTime now() {
        try {
            LocalDateTime simNow = districtRepository.findLatestSimTime();
            if (simNow != null) {
                return simNow;
            }
        } catch (Exception ignored) {
            // district_status 미생성 등 — 실제 시각으로 폴백
        }
        return LocalDateTime.now();
    }
}
