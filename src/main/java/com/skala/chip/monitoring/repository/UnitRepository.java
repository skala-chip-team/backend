package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.UnitMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UnitRepository extends JpaRepository<UnitMaster, String> {

    List<UnitMaster> findByOrderId(String orderId);
}