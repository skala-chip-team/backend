package com.skala.chip.monitoring.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TM_UNIT_MASTER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnitMaster {

    @Id
    @Column(name = "unit_id")
    private String unitId;

    @Column(name = "order_id")
    private String orderId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "district_id")
    private DistrictMaster district;

    @Column(name = "unit_status")
    private String unitStatus;

    @Column(name = "unit_size_qty")
    private Integer unitSizeQty;

    @Column(name = "actual_start_time")
    private LocalDateTime actualStartTime;

    @Column(name = "actual_complete_time")
    private LocalDateTime actualCompleteTime;
}