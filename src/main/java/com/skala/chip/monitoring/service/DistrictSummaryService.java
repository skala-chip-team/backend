package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.DistrictSummaryResponseDTO;
import com.skala.chip.monitoring.dto.ProductionStatusResponseDTO;

public interface DistrictSummaryService {

    DistrictSummaryResponseDTO.DistrictSummary getDistrictSummary(String districtId);

    /** 생산 완료 현황(금일 전체 구역 완성품 수 + 최근 완성 시각). 생산 완료 알림용. */
    ProductionStatusResponseDTO getProductionStatus();
}
