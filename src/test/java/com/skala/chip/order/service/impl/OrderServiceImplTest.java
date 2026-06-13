package com.skala.chip.order.service.impl;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.UnitMaster;
import com.skala.chip.monitoring.repository.ProcessQueueRepository;
import com.skala.chip.monitoring.repository.ProcessStepOrderRepository;
import com.skala.chip.monitoring.repository.ScheduleRepository;
import com.skala.chip.monitoring.repository.UnitRepository;
import com.skala.chip.monitoring.service.SimClock;
import com.skala.chip.order.domain.DailyOrder;
import com.skala.chip.order.dto.OrderResponseDTO;
import com.skala.chip.order.repository.DailyOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @InjectMocks
    private OrderServiceImpl orderServiceImpl;

    @Mock
    private DailyOrderRepository dailyOrderRepository;
    @Mock
    private UnitRepository unitRepository;
    @Mock
    private ProcessQueueRepository processQueueRepository;
    @Mock
    private ProcessStepOrderRepository processStepOrderRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private SimClock simClock;

    private static final LocalDateTime SIM_NOW = LocalDateTime.of(2025, 5, 6, 1, 0);

    private DistrictMaster district;

    @BeforeEach
    void setUp() {
        district = DistrictMaster.builder()
                .districtId("DST-01")
                .districtName("구역 A")
                .build();
    }

    @Test
    @DisplayName("목록 - 유닛 집계로 status/진행률/임박/긴급을 유도한다")
    void 목록_집계_유도() {
        // given
        when(simClock.now()).thenReturn(SIM_NOW);

        // 완료 주문: 2유닛 모두 완료, 오늘 납기, burst
        DailyOrder done = order("ORD-DONE", 1, true, SIM_NOW.plusHours(5));
        // 진행중 주문: 1유닛 완료 + 1유닛 진행중, 내일 납기, 일반
        DailyOrder inProg = order("ORD-PROG", 3, false, SIM_NOW.plusDays(1));
        // 대기 주문: 유닛 모두 대기
        DailyOrder waiting = order("ORD-WAIT", 5, false, SIM_NOW.plusDays(2));

        when(dailyOrderRepository.findAllByOrderByDueDateAsc())
                .thenReturn(List.of(done, inProg, waiting));
        when(unitRepository.findAll()).thenReturn(List.of(
                unit("U-D1", "ORD-DONE", "완료"),
                unit("U-D2", "ORD-DONE", "완료"),
                unit("U-P1", "ORD-PROG", "완료"),
                unit("U-P2", "ORD-PROG", "진행중"),
                unit("U-W1", "ORD-WAIT", "대기"),
                unit("U-W2", "ORD-WAIT", "대기")
        ));

        // when
        OrderResponseDTO.OrderList result = orderServiceImpl.getOrders(null, null);

        // then
        assertThat(result.getTotalCount()).isEqualTo(3);
        assertThat(result.getImminentCount()).isEqualTo(1); // ORD-DONE 만 오늘 납기

        OrderResponseDTO.OrderSummary doneSummary = find(result, "ORD-DONE");
        assertThat(doneSummary.getStatus()).isEqualTo("완료");
        assertThat(doneSummary.getCompletedUnits()).isEqualTo(2);
        assertThat(doneSummary.getProgressRatio()).isEqualTo(1.0);
        assertThat(doneSummary.isDueImminent()).isTrue();
        assertThat(doneSummary.isUrgent()).isTrue();
        assertThat(doneSummary.getPriorityLabel()).isEqualTo("매우 높음");

        OrderResponseDTO.OrderSummary progSummary = find(result, "ORD-PROG");
        assertThat(progSummary.getStatus()).isEqualTo("진행중");
        assertThat(progSummary.getCompletedUnits()).isEqualTo(1);
        assertThat(progSummary.getProgressRatio()).isEqualTo(0.5);
        assertThat(progSummary.isDueImminent()).isFalse();

        OrderResponseDTO.OrderSummary waitSummary = find(result, "ORD-WAIT");
        assertThat(waitSummary.getStatus()).isEqualTo("대기");
        assertThat(waitSummary.getProgressRatio()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("목록 - status 파라미터로 결과를 필터링한다")
    void 목록_status_필터() {
        // given
        when(simClock.now()).thenReturn(SIM_NOW);
        DailyOrder done = order("ORD-DONE", 1, false, SIM_NOW.plusDays(2));
        DailyOrder waiting = order("ORD-WAIT", 5, false, SIM_NOW.plusDays(2));
        when(dailyOrderRepository.findAllByOrderByDueDateAsc())
                .thenReturn(List.of(done, waiting));
        when(unitRepository.findAll()).thenReturn(List.of(
                unit("U-D1", "ORD-DONE", "완료"),
                unit("U-W1", "ORD-WAIT", "대기")
        ));

        // when
        OrderResponseDTO.OrderList result = orderServiceImpl.getOrders("완료", null);

        // then
        assertThat(result.getTotalCount()).isEqualTo(1);
        assertThat(result.getOrders().get(0).getOrderId()).isEqualTo("ORD-DONE");
    }

    private OrderResponseDTO.OrderSummary find(OrderResponseDTO.OrderList list, String orderId) {
        return list.getOrders().stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow();
    }

    private DailyOrder order(String id, int priority, boolean burst, LocalDateTime due) {
        return DailyOrder.builder()
                .orderId(id)
                .district(district)
                .planDate(LocalDate.of(2025, 5, 4))
                .dueDate(due)
                .plannedOutputQty(100)
                .orderPriority(priority)
                .isBurst(burst)
                .build();
    }

    private UnitMaster unit(String unitId, String orderId, String status) {
        return UnitMaster.builder()
                .unitId(unitId)
                .orderId(orderId)
                .unitStatus(status)
                .unitSizeQty(25)
                .build();
    }
}
