package akakash.backend.check;

import java.util.UUID;

public record CheckResult(
    UUID monitorId,
    String status,
    Integer responseTimeMs,
    Integer httpStatusCode,
    String errorMessage
) {}