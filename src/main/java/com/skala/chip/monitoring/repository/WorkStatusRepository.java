package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkStatusRepository extends JpaRepository<WorkStatus, String> {

    /**
     * 금일 생산량(완성품) 집계용: [dayStart, dayEnd) 에 시작된 작업 중 "최종 공정(finalStepId)" 의
     * output_qty 합. 한 unit 이 여러 공정을 거치며 매 step 마다 work_status 가 생기므로, 전 공정을
     * 합치면 완성품 수가 (공정 수)배로 부풀려진다. 최종 공정만 집계해 실제 완성 수량을 구한다.
     */
    @Query("select coalesce(sum(w.outputQty), 0) from WorkStatus w "
            + "where w.machine.district.districtId = :districtId "
            + "and w.startTime >= :dayStart and w.startTime < :dayEnd "
            + "and w.schedule.stepId = :finalStepId")
    long sumFinalStepOutput(@Param("districtId") String districtId,
                            @Param("dayStart") LocalDateTime dayStart,
                            @Param("dayEnd") LocalDateTime dayEnd,
                            @Param("finalStepId") String finalStepId);

    /** 금일 전체 구역 완성품(최종 공정 output) 합. 생산 완료 알림용. */
    @Query("select coalesce(sum(w.outputQty), 0) from WorkStatus w "
            + "where w.startTime >= :dayStart and w.startTime < :dayEnd "
            + "and w.schedule.stepId = :finalStepId")
    long sumFinalStepOutputAll(@Param("dayStart") LocalDateTime dayStart,
                               @Param("dayEnd") LocalDateTime dayEnd,
                               @Param("finalStepId") String finalStepId);

    /** 금일 최종 공정 작업의 가장 최근 시작 시각(sim). 생산 완료 알림용. */
    @Query("select max(w.startTime) from WorkStatus w "
            + "where w.startTime >= :dayStart and w.startTime < :dayEnd "
            + "and w.schedule.stepId = :finalStepId")
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