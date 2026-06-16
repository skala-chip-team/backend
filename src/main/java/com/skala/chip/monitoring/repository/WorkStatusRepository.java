package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, String> {

    /**
     * 금일 생산량(완성품) 집계용: plan_date 가 오늘인 주문에 속한 unit 의 최종 공정(finalStepId) output_qty 합.
     * end_time 날짜 기준 대신 주문 plan_date 기준으로 집계해 전날 지연 완료분이 오늘 달성률에 섞이지 않도록 한다.
     */
    @Query("select coalesce(sum(w.outputQty), 0) from WorkStatus w "
            + "where w.machine.district.districtId = :districtId "
            + "and w.schedule.stepId = :finalStepId "
            + "and w.endTime is not null and w.outputQty > 0 "
            + "and w.schedule.unit.orderId in ("
            + "  select o.orderId from DailyOrder o "
            + "  where o.district.districtId = :districtId "
            + "  and o.planDate = :planDate"
            + ")")
    long sumFinalStepOutput(@Param("districtId") String districtId,
                            @Param("planDate") java.time.LocalDate planDate,
                            @Param("finalStepId") String finalStepId);

    /** 금일 전체 구역 완성품(최종 공정 output) 합. plan_date 기준. */
    @Query("select coalesce(sum(w.outputQty), 0) from WorkStatus w "
            + "where w.schedule.stepId = :finalStepId "
            + "and w.endTime is not null and w.outputQty > 0 "
            + "and w.schedule.unit.orderId in ("
            + "  select o.orderId from DailyOrder o "
            + "  where o.planDate = :planDate"
            + ")")
    long sumFinalStepOutputAll(@Param("planDate") java.time.LocalDate planDate,
                               @Param("finalStepId") String finalStepId);

    /** 금일 최종 공정 작업의 가장 최근 완료 시각(sim). 생산 완료 알림용. */
    @Query("select max(w.endTime) from WorkStatus w "
            + "where w.endTime >= :dayStart and w.endTime < :dayEnd "
            + "and w.schedule.stepId = :finalStepId "
            + "and w.endTime is not null and w.outputQty > 0")
    LocalDateTime latestFinalStepAt(@Param("dayStart") LocalDateTime dayStart,
                                    @Param("dayEnd") LocalDateTime dayEnd,
                                    @Param("finalStepId") String finalStepId);

    List<WorkStatus> findByMachine_MachineId(String machineId);

    // 특정 장비의 기준 시각 이후 작업(=금일 처리 유닛) 조회. 부하율 계산용.
    List<WorkStatus> findByMachine_MachineIdAndStartTimeGreaterThanEqual(String machineId, LocalDateTime startTime);

    List<WorkStatus> findBySchedule_ScheduleId(String scheduleId);

    List<WorkStatus> findByMachine_District_DistrictId(String districtId);

    List<WorkStatus> findByEndTimeIsNull();
}