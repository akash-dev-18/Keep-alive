package akakash.backend.monitor;

import akakash.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "monitors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(length = 2048)
    private String url;

    @Column(name = "ping_key", unique = true)
    private String pingKey;

    @Column(name = "check_interval_min", nullable = false)
    private int checkIntervalMin;

    @Column(name = "http_method")
    private String httpMethod;

    @Column(name = "expected_status")
    private Integer expectedStatus;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds;

    @Column(nullable = false)
    private String status;

    @Column(name = "ssl_days_remaining")
    private Integer sslDaysRemaining;

    @Column(name = "ssl_expires_at")
    private OffsetDateTime sslExpiresAt;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures;

    @Column(name = "alert_after_failures", nullable = false)
    private Integer alertAfterFailures;

    @Column(name = "expected_keyword")
    private String expectedKeyword;

    @Column(name = "basic_auth_username")
    private String basicAuthUsername;

    @Column(name = "basic_auth_password_enc", columnDefinition = "TEXT")
    private String basicAuthPasswordEnc;

    @Column(name = "bearer_token_enc", columnDefinition = "TEXT")
    private String bearerTokenEnc;

    @Column(name = "ssl_issuer")
    private String sslIssuer;

    @Column(name = "expected_next_heartbeat_at")
    private OffsetDateTime expectedNextHeartbeatAt;

    @Column(name = "last_response_time_ms")
    private Integer lastResponseTimeMs;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "last_pinged_at")
    private OffsetDateTime lastPingedAt;

    @Column(name = "next_check_at", nullable = false)
    private OffsetDateTime nextCheckAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = "unknown";
        if (isActive == null) isActive = true;
        if (nextCheckAt == null) nextCheckAt = OffsetDateTime.now();
        if (httpMethod == null) httpMethod = "GET";
        if (expectedStatus == null) expectedStatus = 200;
        if (timeoutSeconds == null) timeoutSeconds = 30;
        if (checkIntervalMin == 0) checkIntervalMin = 2;
        if (consecutiveFailures == null) consecutiveFailures = 0;
        if (alertAfterFailures == null) alertAfterFailures = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}