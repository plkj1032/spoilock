package com.spoilock.backend.user.repository;

import com.spoilock.backend.user.entity.User;

import java.util.Optional;

public interface UserRepositoryCustom {

    Optional<User> findByIdForUpdate(Long userId);

}
