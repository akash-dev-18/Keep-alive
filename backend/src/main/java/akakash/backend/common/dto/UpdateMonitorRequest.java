package akakash.backend.common.dto;

import jakarta.validation.constraints.Min;

public record UpdateMonitorRequest(
    String name,
    String url,
    @Min(1) Integer checkIntervalMin,
    String httpMethod,
    Integer expectedStatus,
    Integer timeoutSeconds,
    String requestHeaders,
    String requestBody,
    Integer alertAfterFailures,
    String expectedKeyword,
    String basicAuthUsername,
    String basicAuthPassword,
    String bearerToken
) {}
