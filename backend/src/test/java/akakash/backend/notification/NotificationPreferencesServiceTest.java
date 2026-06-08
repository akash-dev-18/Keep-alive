package akakash.backend.notification;

import akakash.backend.common.dto.NotificationPreferencesResponse;
import akakash.backend.common.dto.UpdateNotificationPreferencesRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferencesServiceTest {

    @Mock
    private NotificationPreferencesRepository repository;

    @Test
    void shouldSendEmailUsesConfiguredAlertTypeRules() {
        UUID userId = UUID.randomUUID();
        NotificationPreferences prefs = NotificationPreferences.builder()
                .userId(userId)
                .emailOnDown(false)
                .emailOnUp(true)
                .emailOnSslExpiry(false)
                .updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findById(userId)).thenReturn(Optional.of(prefs));

        NotificationPreferencesService service = new NotificationPreferencesService(repository);

        assertThat(service.shouldSendEmail(userId, "down")).isFalse();
        assertThat(service.shouldSendEmail(userId, "cron_missed")).isFalse();
        assertThat(service.shouldSendEmail(userId, "recovery")).isTrue();
        assertThat(service.shouldSendEmail(userId, "ssl_expiry")).isFalse();
        assertThat(service.shouldSendEmail(userId, "custom")).isTrue();
    }

    @Test
    void getPreferencesCreatesDefaultPreferencesWhenMissing() {
        UUID userId = UUID.randomUUID();
        when(repository.findById(userId)).thenReturn(Optional.empty());
        when(repository.save(any(NotificationPreferences.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferencesResponse response = new NotificationPreferencesService(repository).getPreferences(userId);

        assertThat(response.emailOnDown()).isTrue();
        assertThat(response.emailOnUp()).isTrue();
        assertThat(response.emailOnSslExpiry()).isTrue();
        verify(repository).save(any(NotificationPreferences.class));
    }

    @Test
    void updatePreferencesOnlyChangesProvidedFields() {
        UUID userId = UUID.randomUUID();
        NotificationPreferences prefs = NotificationPreferences.builder()
                .userId(userId)
                .emailOnDown(true)
                .emailOnUp(true)
                .emailOnSslExpiry(true)
                .updatedAt(OffsetDateTime.now())
                .build();
        when(repository.findById(userId)).thenReturn(Optional.of(prefs));
        when(repository.save(prefs)).thenReturn(prefs);

        NotificationPreferencesResponse response = new NotificationPreferencesService(repository)
                .updatePreferences(userId, new UpdateNotificationPreferencesRequest(false, null, false));

        assertThat(response.emailOnDown()).isFalse();
        assertThat(response.emailOnUp()).isTrue();
        assertThat(response.emailOnSslExpiry()).isFalse();
    }
}
