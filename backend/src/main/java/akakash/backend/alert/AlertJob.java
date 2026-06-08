package akakash.backend.alert;

import java.util.UUID;

public record AlertJob(
        UUID monitorId,
        UUID incidentId,
        String alertType,
        String recipientEmail,
        String monitorName
) {}