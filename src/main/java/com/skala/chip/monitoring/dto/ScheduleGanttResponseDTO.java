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
     * estimatedStart ~ estimatedEnd 구간으로 막대를 렌더링한다.
     * estimatedEnd = estimatedStart + step_avg_time(분).
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
    }
}
