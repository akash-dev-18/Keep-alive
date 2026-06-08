package akakash.backend.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record MonitorResponse(
    UUID id,
    String name,
    String type,
    String url,
    String pingKey,
    int checkIntervalMin,
    String httpMethod,
    int expectedStatus,
    int timeoutSeconds,
    String status,
    @JsonProperty("active") boolean isActive,
    OffsetDateTime createdAt,
    String requestHeaders,
    String requestBody,
    Integer sslDaysRemaining,
    OffsetDateTime sslExpiresAt,
    String sslIssuer,
    OffsetDateTime lastCheckedAt,
    OffsetDateTime lastPingedAt,
    OffsetDateTime expectedNextHeartbeatAt,
    Integer lastResponseTimeMs,
    int consecutiveFailures,
    int alertAfterFailures,
    String expectedKeyword,
    String basicAuthUsername,
    @JsonProperty("hasBasicAuth") boolean hasBasicAuth,
    @JsonProperty("hasBearerToken") boolean hasBearerToken
) {
    public Map<String, String> getRequestHeadersMap() {
        if (requestHeaders == null || requestHeaders.isBlank()) return Collections.emptyMap();
        try {
            return new ObjectMapper().readValue(requestHeaders, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
}
