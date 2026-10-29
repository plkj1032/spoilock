package com.spoilock.backend.notification.repository;

import com.spoilock.backend.notification.domain.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

}