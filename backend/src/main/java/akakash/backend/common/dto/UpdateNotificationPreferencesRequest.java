package akakash.backend.common.dto;

public record UpdateNotificationPreferencesRequest(
    Boolean emailOnDown,
    Boolean emailOnUp,
    Boolean emailOnSslExpiry
) {}
