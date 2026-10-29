package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByNicknameKey(String nicknameKey);
}
