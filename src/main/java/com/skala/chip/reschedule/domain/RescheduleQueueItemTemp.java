package com.skala.chip.reschedule.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "reschedule_queue_item_temp")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleQueueItemTemp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    private String optionId;

    private String queueId;

    private String unitId;

    private String stepId;

    private String districtId;

    private Integer beforePosition;

    private Integer afterPosition;

    private Double score;

    @Column(columnDefinition = "TEXT")
    private String reason;
}