package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, String> {

    /**
     * 금일 생산량(완성품) 집계용: [dayStart, dayEnd) 에 완료된 작업 중 "최종 공정(finalStepId)" 의
     * output_qty 합. end_time 기준으로 집계해 plan_date(완료 예정일)와 비교 기준을 맞춘다.
     * start_time 기준으로 집계하면 전날 시작해 오늘 완료된 작업이 누락되고 기준이 어긋난다.
     * end_time IS NOT NULL + outputQty > 0 조건으로 진행중·실패(재처리) 행을 제외한다.
     */
    @Query("select coalesce(sum(w.outputQty), 0) from WorkStatus w "
            + "where w.machine.district.districtId = :districtId "
            + "and w.endTime >= :dayStart and w.endTime < :dayEnd "
            + "and w.schedule.stepId = :finalStepId "
            + "and w.endTime is not null and w.outputQty > 0")
    long sumFinalStepOutput(@Param("districtId") String districtId,
                            @Param("dayStart") LocalDateTime dayStart,
                            @Param("dayEnd") LocalDateTime dayEnd,
                            @Param("finalStepId") String finalStepId);

    /** 금일 전체 구역 완성품(최종 공정 output) 합. end_time 완료 기준. */
    @Query("select coalesce(sum(w.outputQty), 0) from WorkStatus w "
            + "where w.endTime >= :dayStart and w.endTime < :dayEnd "
            + "and w.schedule.stepId = :finalStepId "
            + "and w.endTime is not null and w.outputQty > 0")
    long sumFinalStepOutputAll(@Param("dayStart") LocalDateTime dayStart,
                               @Param("dayEnd") LocalDateTime dayEnd,
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