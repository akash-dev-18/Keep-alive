package akakash.backend.check;

import java.util.UUID;

public record CheckJob(
    UUID monitorId,
    String type
) {}