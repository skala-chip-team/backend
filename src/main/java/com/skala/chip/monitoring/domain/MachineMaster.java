package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "machine_master")
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

    @Column(name = "rated_capacity")
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