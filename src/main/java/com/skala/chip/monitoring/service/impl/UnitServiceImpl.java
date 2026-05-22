package com.skala.chip.monitoring.service.impl;

import com.skala.chip.monitoring.domain.UnitMaster;
import com.skala.chip.monitoring.dto.UnitResponseDTO;
import com.skala.chip.monitoring.repository.UnitRepository;
import com.skala.chip.monitoring.service.UnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnitServiceImpl implements UnitService {

    private final UnitRepository unitRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UnitResponseDTO.UnitInfo> getUnits() {

        List<UnitMaster> units = unitRepository.findAll();

        return units.stream()
                .map(UnitResponseDTO.UnitInfo::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnitResponseDTO.UnitInfo getUnit(String unitId) {

        UnitMaster unit = unitRepository.findById(unitId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "유닛을 찾을 수 없습니다. unitId=" + unitId
                        ));

        return UnitResponseDTO.UnitInfo.from(unit);
    }
}