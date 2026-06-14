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

    public Notification createNotification(Long userId, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
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

    // --- AUTOMATED NOTIFICATION TRIGGERS ---

    public void notifyAdminsOnRegistration(String username) {
        userRepository.findAll().stream()
            .filter(u -> u.getRole() == User.Role.SUPERADMIN)
            .forEach(admin -> {
                createNotification(admin.getId(), "User Baru", "User @" + username + " baru saja mendaftar dan menunggu persetujuan.");
            });
    }

    public void notifyTeacherOnJoin(Long teacherId, String studentName, String className) {
        createNotification(teacherId, "Murid Bergabung", "Murid " + studentName + " bergabung ke kelas " + className);
    }

    public void notifyStudentsOnNewMaterial(ClassGroup classGroup, String materialTitle) {
        String title = "Materi Baru";
        String msg = "Guru telah merilis materi baru: \"" + materialTitle + "\" di kelas " + classGroup.getName();
        if (classGroup.getMembers() != null) {
            for (ClassMember cm : classGroup.getMembers()) {
                try {
                    createNotification(cm.getUser().getId(), title, msg);
                } catch (Exception e) {}
            }
        }
    }

    public void notifyUserOnGenerationComplete(Long userId, String materialTitle) {
        createNotification(userId, "AI Selesai", "Generate AI untuk materi \"" + materialTitle + "\" telah selesai.");
    }
}
