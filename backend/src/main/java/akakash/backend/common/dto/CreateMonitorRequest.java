package akakash.backend.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMonitorRequest(
    @NotBlank String name,
    @NotBlank String type,
    String url,
    @NotNull @Min(1) Integer checkIntervalMin,
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
