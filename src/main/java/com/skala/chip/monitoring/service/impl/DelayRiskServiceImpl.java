package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.DelayRisk;
import com.skala.chip.monitoring.dto.DelayRiskResponseDTO;
import com.skala.chip.monitoring.repository.DelayRiskRepository;
import com.skala.chip.monitoring.service.DelayRiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DelayRiskServiceImpl implements DelayRiskService {

    private final DelayRiskRepository delayRiskRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DelayRiskResponseDTO.RiskInfo> getRisks() {

        List<DelayRisk> risks = delayRiskRepository.findAll();

        return risks.stream()
                .map(DelayRiskResponseDTO.RiskInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelayRiskResponseDTO.RiskInfo> getHighRisks() {

        List<DelayRisk> risks =
                delayRiskRepository.findByRiskLevelIn(
                        List.of("HIGH", "CRITICAL")
                );

        return risks.stream()
                .map(DelayRiskResponseDTO.RiskInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelayRiskResponseDTO.RiskInfo> getUnitRisks(String unitId) {

        List<DelayRisk> risks =
                delayRiskRepository.findByUnit_UnitId(unitId);

        return risks.stream()
                .map(DelayRiskResponseDTO.RiskInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelayRiskResponseDTO.RiskInfo> getDistrictRisks(String districtId) {

        List<DelayRisk> risks =
                delayRiskRepository.findByDistrict_DistrictId(districtId);

        return risks.stream()
                .map(DelayRiskResponseDTO.RiskInfo::from)
                .toList();
    }
}