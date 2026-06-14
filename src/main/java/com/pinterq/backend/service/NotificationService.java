package com.pinterq.backend.service;

import com.pinterq.backend.model.ClassGroup;
import com.pinterq.backend.model.ClassMember;
import com.pinterq.backend.model.Notification;
import com.pinterq.backend.model.User;
import com.pinterq.backend.repository.ClassGroupRepository;
import com.pinterq.backend.repository.ClassMemberRepository;
import com.pinterq.backend.repository.NotificationRepository;
import com.pinterq.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public Notification createNotification(Long userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .isRead(false)
                .build();
        return notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public Notification markRead(Long notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notif.setIsRead(true);
        return notificationRepository.save(notif);
    }

    public int countUnread(Long userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false).size();
    }

    // Convenience methods for triggering notifications in class workflows
    public void notifyTeacherOnJoin(Long teacherId, String studentName, String className) {
        createNotification(teacherId, "Murid " + studentName + " bergabung ke kelas " + className);
    }

    public void notifyStudentsOnNewQuiz(ClassGroup classGroup, String studentName) {
        String msg = "Materi baru di kelas " + classGroup.getName();
        for (ClassMember cm : classGroup.getMembers()) {
            try {
                createNotification(cm.getUser().getId(), msg);
            } catch (Exception e) {
                // skip if user not found
            }
        }
    }
}
