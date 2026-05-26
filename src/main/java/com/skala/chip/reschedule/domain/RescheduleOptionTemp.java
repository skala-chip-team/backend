package com.skala.chip.reschedule.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reschedule_option_temp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleOptionTemp {

    @Id
    private String optionId;

    private String targetQueueId;

    private String perspective;

    private String status;

    private LocalDateTime createdAt;
}