package com.skala.chip.monitoring.service;

import com.skala.chip.monitoring.dto.DistrictSummaryResponseDTO;

public interface DistrictSummaryService {

    DistrictSummaryResponseDTO.DistrictSummary getDistrictSummary(String districtId);
}
