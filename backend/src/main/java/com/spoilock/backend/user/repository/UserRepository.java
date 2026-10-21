package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existByEmail(String email);
    Optional<User> findByEmail(String email);
}
