package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.UnitResponseDTO;

import java.util.List;

public interface UnitService {

    List<UnitResponseDTO.UnitInfo> getUnits();

    UnitResponseDTO.UnitInfo getUnit(String unitId);
}