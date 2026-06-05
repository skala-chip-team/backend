package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.ProcessStepOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessStepOrderRepository extends JpaRepository<ProcessStepOrder, String> {
}
