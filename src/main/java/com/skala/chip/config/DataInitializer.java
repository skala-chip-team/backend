package com.skala.chip.config;

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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        UserRole adminRole = userRoleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> userRoleRepository.save(
                        UserRole.builder()
                                .roleId("ROLE_ADMIN")
                                .roleName("ADMIN")
                                .description("총괄관리자")
                                .build()
                ));

        if (userRepository.existsByEmail("admin@skala.com")) return;

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
}
