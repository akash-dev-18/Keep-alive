package akakash.backend.common.dto;

import akakash.backend.alert.AlertLog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertLogResponse(
        UUID id,
        UUID monitorId,
        UUID incidentId,
        String alertType,
        String channel,
        String sentTo,
        String status,
        String errorMessage,
        OffsetDateTime sentAt
) {
    public static AlertLogResponse fromEntity(AlertLog a) {
        return new AlertLogResponse(
                a.getId(), a.getMonitorId(), a.getIncidentId(),
                a.getAlertType(), a.getChannel(), a.getSentTo(),
                a.getStatus(), a.getErrorMessage(), a.getSentAt()
        );
    }
}
