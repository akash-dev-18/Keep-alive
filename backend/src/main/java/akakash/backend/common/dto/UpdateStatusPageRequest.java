package akakash.backend.common.dto;

import java.util.UUID;

public record UpdateStatusPageRequest(
        String name,
        String slug,
        UUID[] monitorIds,
        Boolean isPublic,
        String customDomain,
        String description,
        String logoUrl,
        String primaryColor
) {}
