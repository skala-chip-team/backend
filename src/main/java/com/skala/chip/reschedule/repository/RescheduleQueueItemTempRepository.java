package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleQueueItemTemp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RescheduleQueueItemTempRepository
        extends JpaRepository<RescheduleQueueItemTemp, Long> {

    List<RescheduleQueueItemTemp> findByOptionId(String optionId);
}