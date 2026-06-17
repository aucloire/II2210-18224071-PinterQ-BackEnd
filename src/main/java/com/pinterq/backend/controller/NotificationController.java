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
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestParam Long userId) {
        try {
            List<Notification> notifications = notificationService.getNotifications(userId);
            List<Map<String, Object>> response = notifications.stream().map(n -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", n.getId());
                map.put("message", n.getMessage() != null ? n.getMessage() : "");
                map.put("isRead", n.getIsRead() != null ? n.getIsRead() : false);
                map.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
                return map;
            }).toList();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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
