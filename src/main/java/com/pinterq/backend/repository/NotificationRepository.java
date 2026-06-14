package com.pinterq.backend.repository;

import com.pinterq.backend.model.Notification;
import com.pinterq.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    List<Notification> findByUser_IdAndIsReadOrderByCreatedAtDesc(Long userId, Boolean isRead);
}
