package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.ScheduleGanttResponseDTO;

public interface ScheduleGanttService {

    ScheduleGanttResponseDTO.DistrictGantt getDistrictGantt(String districtId);
}
