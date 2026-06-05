package com.skala.chip.reschedule.repository;

import com.skala.chip.reschedule.domain.RescheduleConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RescheduleConfigRepository extends JpaRepository<RescheduleConfig, String> {
}
