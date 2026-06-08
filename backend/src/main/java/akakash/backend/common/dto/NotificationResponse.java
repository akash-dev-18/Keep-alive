package akakash.backend.common.dto;

import akakash.backend.notification.Notification;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID monitorId,
        UUID incidentId,
        String title,
        String body,
        boolean isRead,
        OffsetDateTime createdAt
) {
    public static NotificationResponse fromEntity(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getMonitorId(),
                n.getIncidentId(),
                n.getTitle(),
                n.getBody(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}