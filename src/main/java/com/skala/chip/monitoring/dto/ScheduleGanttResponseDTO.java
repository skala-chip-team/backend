package com.skala.chip.monitoring.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

public class ScheduleGanttResponseDTO {

    /**
     * 구역 단위 간트 차트. step_order 순으로 정렬된 공정 단계 목록을 가진다.
     */
    @Getter
    @Builder
    public static class DistrictGantt {

        private String districtId;
        private String districtName;

        // 시뮬레이션 스냅샷 시각(sim 기준). 프론트가 경과시간 계산·재시뮬 권장 판단에 사용. 데이터 없으면 실제 시각.
        private LocalDateTime simulatedAt;

        private List<StepGantt> steps;
    }

    /**
     * 공정 단계(step)별 간트. 해당 단계에 속한 장비 스케줄 막대 목록을 가진다.
     */
    @Getter
    @Builder
    public static class StepGantt {

        private String stepId;
        private String processStep;   // 공정 단계명
        private Integer stepOrder;    // 공정 순서
        private Integer stepAvgTime;  // 평균 소요시간 (분)

        private List<GanttBar> schedules;
    }

    /**
     * 간트 차트의 막대 한 개 (스케줄 1건).
     *
     * 렌더링 우선순위(프론트):
     *  - 막대 시작 = actualStart ?? estimatedStart
     *  - 막대 종료 = actualEnd ?? projectedEnd ?? estimatedEnd
     *
     * estimatedStart/estimatedEnd : 계획(예측) 구간. estimatedEnd = estimatedStart + step_avg_time(분).
     * actualStart/actualEnd       : 실제 작업 상태(work_status)의 실제 시작/종료. 없으면 null.
     * projectedEnd                : 진행 중(실제 시작 O, 실제 종료 X)일 때 actualStart + step_avg_time 로 산출한 예상 종료.
     */
    @Getter
    @Builder
    public static class GanttBar {

        private String scheduleId;

        private String machineId;
        private String machineStatus;

        private String unitId;
        private String unitStatus;

        private Integer priority;
        private String status;
        private Boolean active;

        private LocalDateTime estimatedStart;
        private LocalDateTime estimatedEnd;

        private LocalDateTime actualStart;
        private LocalDateTime actualEnd;
        private LocalDateTime projectedEnd;

        // 승인된 재조정(reschedule_selection)으로 이 (unit, step) 스케줄이 변경됐는지 여부.
        // 계획 간트에서 "재조정 반영됨"을 색으로 구분하기 위함.
        private boolean rescheduled;
    }
}
