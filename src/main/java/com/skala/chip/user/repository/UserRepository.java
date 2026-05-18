package com.skala.chip.user.repository;

import com.skala.chip.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User 엔티티 데이터 접근 인터페이스.
 *
 * JpaRepository<User, String>: PK 타입이 String인 이유는
 * ERD의 user_id가 varchar로 정의되어 있기 때문이다.
 */
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 로그인 시 이메일로 사용자를 조회한다.
     *
     * Optional을 반환하는 이유:
     * 존재하지 않는 이메일로 조회 시 null 대신 Optional.empty()를 반환해
     * 호출부에서 명시적으로 존재 여부를 처리하도록 강제한다.
     */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
