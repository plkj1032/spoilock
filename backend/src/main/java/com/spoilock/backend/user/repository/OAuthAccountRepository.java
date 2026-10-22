package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.OAuthAccount;
import com.spoilock.backend.user.enums.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthAccountRepository
        extends JpaRepository<OAuthAccount, Long> {

    Optional<OAuthAccount> findByProviderAndProviderUserId(
            OAuthProvider provider,
            String providerUserId
    );
}
