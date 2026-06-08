package akakash.backend.common.dto;

import akakash.backend.notification.NotificationPreferences;

import java.time.OffsetDateTime;

public record NotificationPreferencesResponse(
        boolean emailOnDown,
        boolean emailOnUp,
        boolean emailOnSslExpiry,
        OffsetDateTime updatedAt
) {
    public static NotificationPreferencesResponse fromEntity(NotificationPreferences prefs) {
        return new NotificationPreferencesResponse(
                prefs.isEmailOnDown(),
                prefs.isEmailOnUp(),
                prefs.isEmailOnSslExpiry(),
                prefs.getUpdatedAt()
        );
    }
}