package com.skala.chip.monitoring.repository;

import com.skala.chip.monitoring.domain.DistrictMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DistrictRepository extends JpaRepository<DistrictMaster, String> {
}