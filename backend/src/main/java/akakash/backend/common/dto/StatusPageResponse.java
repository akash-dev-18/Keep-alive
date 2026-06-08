package akakash.backend.common.dto;

import akakash.backend.statuspage.StatusPage;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StatusPageResponse(
        UUID id,
        String name,
        String slug,
        UUID[] monitorIds,
        boolean isPublic,
        String customDomain,
        String description,
        String logoUrl,
        String primaryColor,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static StatusPageResponse fromEntity(StatusPage sp) {
        return new StatusPageResponse(
                sp.getId(),
                sp.getName(),
                sp.getSlug(),
                sp.getMonitorIds(),
                sp.getIsPublic(),
                sp.getCustomDomain(),
                sp.getDescription(),
                sp.getLogoUrl(),
                sp.getPrimaryColor(),
                sp.getCreatedAt(),
                sp.getUpdatedAt()
        );
    }
}
