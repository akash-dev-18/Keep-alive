package akakash.backend.common.dto;

import akakash.backend.check.MonitorCheck;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MonitorCheckResponse(
        UUID id,
        UUID monitorId,
        String status,
        Integer responseTimeMs,
        Integer httpStatusCode,
        String errorMessage,
        OffsetDateTime checkedAt
) {
    public static MonitorCheckResponse fromEntity(MonitorCheck mc) {
        return new MonitorCheckResponse(
                mc.getId(),
                mc.getMonitorId(),
                mc.getStatus(),
                mc.getResponseTimeMs(),
                mc.getHttpStatusCode(),
                mc.getErrorMessage(),
                mc.getCheckedAt()
        );
    }
}