package com.pinterq.backend.controller;

import com.pinterq.backend.model.Notification;
import com.pinterq.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = {"https://aucloire.stei.my.id", "http://localhost:5173"}, allowedHeaders = "*", allowCredentials = "true")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestParam Long userId) {
        List<Notification> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(notifications.stream().map(n -> Map.of(
                "id", n.getId(),
                "message", n.getMessage(),
                "isRead", n.getIsRead(),
                "createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : ""
        )).toList());
    }

    @PutMapping("/{notificationId}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Long notificationId) {
        Notification notif = notificationService.markRead(notificationId);
        return ResponseEntity.ok(Map.of(
                "id", notif.getId(),
                "isRead", notif.getIsRead()
        ));
    }
}
