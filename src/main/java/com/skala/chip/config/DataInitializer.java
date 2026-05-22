package com.skala.chip.config;

import com.skala.chip.monitoring.domain.DistrictMaster;
import com.skala.chip.monitoring.domain.MachineMaster;
import com.skala.chip.monitoring.repository.DistrictRepository;
import com.skala.chip.monitoring.repository.MachineRepository;
import com.skala.chip.user.domain.User;
import com.skala.chip.user.domain.UserRole;
import com.skala.chip.user.repository.UserRepository;
import com.skala.chip.user.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final DistrictRepository districtRepository;
    private final MachineRepository machineRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initAdminUser();
        initDistricts();
        initMachines();
    }

    private void initAdminUser() {
        UserRole adminRole = userRoleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> userRoleRepository.save(
                        UserRole.builder()
                                .roleId("ROLE_ADMIN")
                                .roleName("ADMIN")
                                .description("총괄관리자")
                                .build()
                ));

        if (userRepository.existsByEmail("admin@skala.com")) {
            return;
        }

        userRepository.save(User.builder()
                .userId(UUID.randomUUID().toString())
                .role(adminRole)
                .username("admin")
                .email("admin@skala.com")
                .passwordHash(passwordEncoder.encode("admin1234"))
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void initDistricts() {
        if (districtRepository.existsById("DST-01")) {
            return;
        }

        districtRepository.save(DistrictMaster.builder()
                .districtId("DST-01")
                .districtName("제1구역")
                .commanderId("CMD-001")
                .build());

        districtRepository.save(DistrictMaster.builder()
                .districtId("DST-02")
                .districtName("제2구역")
                .commanderId("CMD-002")
                .build());
    }

    private void initMachines() {
        if (machineRepository.existsById("MACHINE-01")) {
            return;
        }

        DistrictMaster district1 = districtRepository.findById("DST-01")
                .orElseThrow();
        DistrictMaster district2 = districtRepository.findById("DST-02")
                .orElseThrow();

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-01")
                .machineType("TYPE_A")
                .district(district1)
                .maker("Siemens")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-02")
                .machineType("TYPE_A")
                .district(district1)
                .maker("Siemens")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-03")
                .machineType("TYPE_A")
                .district(district1)
                .maker("Fanuc")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-04")
                .machineType("TYPE_A")
                .district(district1)
                .maker("ABB")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-05")
                .machineType("TYPE_A")
                .district(district2)
                .maker("ABB")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-06")
                .machineType("TYPE_A")
                .district(district2)
                .maker("ABB")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());

        machineRepository.save(MachineMaster.builder()
                .machineId("MACHINE-07")
                .machineType("TYPE_A")
                .district(district2)
                .maker("Siemens")
                .ratedCapacity(150)
                .installYear(2023)
                .machineStatus("대기")
                .build());
    }
}