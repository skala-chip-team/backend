package com.skala.chip.monitoring.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "machine_status_event")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MachineStatusEvent {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "machine_id")
    private String machineId;

    @Column(name = "status")
    private String status;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;
}
