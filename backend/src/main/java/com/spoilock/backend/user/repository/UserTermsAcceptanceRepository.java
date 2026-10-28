package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.UserTermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsAcceptanceRepository
    extends JpaRepository<UserTermsAcceptance, Long> {
}
