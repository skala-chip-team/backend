package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TM_MACHINE_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MachineMaster {

    @Id
    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "machine_type")
    private String machineType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id")
    private DistrictMaster district;

    @Column(name = "maker")
    private String maker;

    // 실제 테이블 컬럼명은 daily_capacity (구 rated_capacity). 필드명은 호환 위해 유지.
    @Column(name = "daily_capacity")
    private Integer ratedCapacity;

    @Column(name = "install_year")
    private Integer installYear;

    @Column(name = "machine_status")
    private String machineStatus;

    // 추가
    public void updateStatus(String machineStatus) {
        this.machineStatus = machineStatus;
    }
}