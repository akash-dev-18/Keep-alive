package akakash.backend.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStatusPageRequest(
        @NotBlank String name,
        @NotBlank String slug,
        @NotNull UUID[] monitorIds,
        Boolean isPublic,
        String customDomain,
        String description,
        String logoUrl,
        String primaryColor
) {}
