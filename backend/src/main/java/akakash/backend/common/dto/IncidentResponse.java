package akakash.backend.common.dto;

import akakash.backend.incident.Incident;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        UUID monitorId,
        String status,
        OffsetDateTime startedAt,
        OffsetDateTime resolvedAt,
        Integer durationSeconds,
        String cause
) {
    public static IncidentResponse fromEntity(Incident i) {
        return new IncidentResponse(
                i.getId(),
                i.getMonitorId(),
                i.getStatus(),
                i.getStartedAt(),
                i.getResolvedAt(),
                i.getDurationSeconds(),
                i.getCause()
        );
    }
}