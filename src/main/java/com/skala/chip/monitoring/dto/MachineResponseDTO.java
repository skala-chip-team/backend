package com.skala.chip.monitoring.dto;

import com.skala.chip.monitoring.domain.MachineMaster;
import lombok.Builder;
import lombok.Getter;

public class MachineResponseDTO {

    @Getter
    @Builder
    public static class MachineInfo {
        private String machineId;
        private String machineType;
        private String districtId;
        private String districtName;
        private String maker;
        private Integer ratedCapacity;
        private Integer installYear;
        private String machineStatus;

        public static MachineInfo from(MachineMaster machine) {
            return MachineInfo.builder()
                    .machineId(machine.getMachineId())
                    .machineType(machine.getMachineType())
                    .districtId(machine.getDistrict() != null ? machine.getDistrict().getDistrictId() : null)
                    .districtName(machine.getDistrict() != null ? machine.getDistrict().getDistrictName() : null)
                    .maker(machine.getMaker())
                    .ratedCapacity(machine.getRatedCapacity())
                    .installYear(machine.getInstallYear())
                    .machineStatus(machine.getMachineStatus())
                    .build();
        }
    }
}