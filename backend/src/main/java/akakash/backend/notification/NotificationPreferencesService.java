package akakash.backend.notification;

import akakash.backend.common.dto.NotificationPreferencesResponse;
import akakash.backend.common.dto.UpdateNotificationPreferencesRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final NotificationPreferencesRepository repository;

    public NotificationPreferencesResponse getPreferences(UUID userId) {
        return NotificationPreferencesResponse.fromEntity(getOrCreate(userId));
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(UUID userId,
                                                             UpdateNotificationPreferencesRequest request) {

        NotificationPreferences prefs = getOrCreate(userId);

        if (request.emailOnDown() != null) {
            prefs.setEmailOnDown(request.emailOnDown());
        }
        if (request.emailOnUp() != null) {
            prefs.setEmailOnUp(request.emailOnUp());
        }
        if (request.emailOnSslExpiry() != null) {
            prefs.setEmailOnSslExpiry(request.emailOnSslExpiry());
        }

        return NotificationPreferencesResponse.fromEntity(repository.save(prefs));
    }

    public boolean shouldSendEmail(UUID userId, String alertType) {
        NotificationPreferences prefs = getOrCreate(userId);

        return switch (alertType) {
            case "down", "cron_missed" -> prefs.isEmailOnDown();
            case "recovery" -> prefs.isEmailOnUp();
            case "ssl_expiry" -> prefs.isEmailOnSslExpiry();
            default -> true;
        };
    }

    private NotificationPreferences getOrCreate(UUID userId) {
        return repository.findById(userId)
                .orElseGet(() -> repository.save(
                        NotificationPreferences.builder()
                                .userId(userId)
                                .emailOnDown(true)
                                .emailOnUp(true)
                                .emailOnSslExpiry(true)
                                .build()
                ));
    }
}