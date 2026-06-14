package com.pinterq.backend.repository;

import com.pinterq.backend.model.Notification;
import com.pinterq.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserId(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = :isRead ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndIsRead(@Param("userId") Long userId, @Param("isRead") Boolean isRead);
}
