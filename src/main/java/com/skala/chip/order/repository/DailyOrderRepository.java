package com.skala.chip.order.repository;

import com.skala.chip.order.domain.DailyOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyOrderRepository extends JpaRepository<DailyOrder, String> {

    // 납기 오름차순(임박 순) 전체 조회
    List<DailyOrder> findAllByOrderByDueDateAsc();

    // 특정 구역의 계획일(plan_date) 기준 주문 조회. 구역별 일일 생산 목표량 집계용.
    List<DailyOrder> findByDistrict_DistrictIdAndPlanDate(String districtId, LocalDate planDate);
}
