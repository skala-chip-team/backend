package com.skala.chip.order.repository;

import com.skala.chip.order.domain.DailyOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DailyOrderRepository extends JpaRepository<DailyOrder, String> {

    // 납기 오름차순(임박 순) 전체 조회
    List<DailyOrder> findAllByOrderByDueDateAsc();
}
