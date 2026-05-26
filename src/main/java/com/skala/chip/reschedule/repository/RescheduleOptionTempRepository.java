package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleOptionTemp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RescheduleOptionTempRepository
        extends JpaRepository<RescheduleOptionTemp, String> {
}