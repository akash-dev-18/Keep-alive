package akakash.backend.notification;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void createSavesUnreadNotification() {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();

        new NotificationService(notificationRepository)
                .create(userId, monitorId, incidentId, "Down", "API is down");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(notificationCaptor.getValue().getMonitorId()).isEqualTo(monitorId);
        assertThat(notificationCaptor.getValue().getIncidentId()).isEqualTo(incidentId);
        assertThat(notificationCaptor.getValue().getIsRead()).isFalse();
    }

    @Test
    void markAsReadRejectsNotificationOwnedByAnotherUser() {
        UUID notificationId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(ownerId)
                .build();
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> new NotificationService(notificationRepository).markAsRead(notificationId, otherUserId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unauthorized");
    }
}
