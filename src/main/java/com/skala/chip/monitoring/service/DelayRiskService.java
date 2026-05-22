package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.DelayRiskResponseDTO;

import java.util.List;

public interface DelayRiskService {

    List<DelayRiskResponseDTO.RiskInfo> getRisks();

    List<DelayRiskResponseDTO.RiskInfo> getHighRisks();

    List<DelayRiskResponseDTO.RiskInfo> getUnitRisks(String unitId);

    List<DelayRiskResponseDTO.RiskInfo> getDistrictRisks(String districtId);
}