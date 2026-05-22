package com.skala.chip.monitoring.dto;

import com.skala.chip.monitoring.domain.UnitMaster;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class UnitResponseDTO {

    @Getter
    @Builder
    public static class UnitInfo {

        private String unitId;
        private String orderId;

        private String districtId;
        private String districtName;

        private String unitStatus;
        private Integer unitSizeQty;

        private LocalDateTime actualStartTime;
        private LocalDateTime actualCompleteTime;

        public static UnitInfo from(UnitMaster unit) {

            return UnitInfo.builder()
                    .unitId(unit.getUnitId())
                    .orderId(unit.getOrderId())

                    .districtId(
                            unit.getDistrict() != null
                                    ? unit.getDistrict().getDistrictId()
                                    : null
                    )
                    .districtName(
                            unit.getDistrict() != null
                                    ? unit.getDistrict().getDistrictName()
                                    : null
                    )

                    .unitStatus(unit.getUnitStatus())
                    .unitSizeQty(unit.getUnitSizeQty())

                    .actualStartTime(unit.getActualStartTime())
                    .actualCompleteTime(unit.getActualCompleteTime())

                    .build();
        }
    }
}