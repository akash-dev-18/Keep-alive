package akakash.backend.notification;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void create(UUID userId,
                       UUID monitorId,
                       UUID incidentId,
                       String title,
                       String body) {

        Notification notification = Notification.builder()
                .userId(userId)
                .monitorId(monitorId)
                .incidentId(incidentId)
                .title(title)
                .body(body)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    public List<Notification> getNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByIsReadAscCreatedAtDesc(userId);
    }

    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }

        notificationRepository.markAsReadById(notificationId);
    }
}
