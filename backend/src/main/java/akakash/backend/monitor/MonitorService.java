package akakash.backend.monitor;

import akakash.backend.common.dto.CreateMonitorRequest;
import akakash.backend.common.dto.MonitorResponse;
import akakash.backend.common.dto.UpdateMonitorRequest;
import akakash.backend.common.enums.MonitorStatus;
import akakash.backend.common.enums.MonitorType;
import akakash.backend.common.exception.PlanLimitException;
import akakash.backend.common.exception.ResourceNotFoundException;
import akakash.backend.common.validation.MonitorValidator;
import akakash.backend.config.SecretEncryptionService;
import akakash.backend.user.Plan;
import akakash.backend.user.User;
import akakash.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MonitorService {

    private static final int FREE_MONITOR_LIMIT = 3;
    private static final int PRO_MONITOR_LIMIT = 50;
    private static final int FREE_MIN_INTERVAL = 2;
    private static final int PRO_MIN_INTERVAL = 1;

    private final MonitorRepository monitorRepository;
    private final UserRepository userRepository;
    private final SecretEncryptionService encryptionService;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public MonitorResponse createMonitor(UUID userId, CreateMonitorRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        MonitorType type = MonitorType.fromString(request.type());

        validatePlanLimits(user, userId, request.checkIntervalMin());
        validateMonitorFields(
                type,
                request.url(),
                request.httpMethod(),
                request.timeoutSeconds(),
                request.alertAfterFailures(),
                request.expectedKeyword()
        );

        Monitor monitor = Monitor.builder()
                .user(user)
                .name(request.name().trim())
                .type(type.getDbValue())
                .url(request.url() != null ? request.url().trim() : null)
                .httpMethod(request.httpMethod() != null ? request.httpMethod().toUpperCase() : "GET")
                .expectedStatus(request.expectedStatus() != null ? request.expectedStatus() : 200)
                .timeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : 30)
                .checkIntervalMin(request.checkIntervalMin())
                .requestHeaders(request.requestHeaders())
                .requestBody(request.requestBody())
                .alertAfterFailures(request.alertAfterFailures() != null ? request.alertAfterFailures() : 1)
                .expectedKeyword(request.expectedKeyword())
                .basicAuthUsername(request.basicAuthUsername())
                .status(MonitorStatus.UNKNOWN.getDbValue())
                .isActive(true)
                .consecutiveFailures(0)
                .lastResponseTimeMs(null)
                .nextCheckAt(OffsetDateTime.now())
                .build();

        applyAuthFields(monitor, request.basicAuthPassword(), request.bearerToken());

        if (type == MonitorType.HEARTBEAT) {
            monitor.setPingKey(UUID.randomUUID().toString().replace("-", ""));
        }

        monitor = monitorRepository.save(monitor);

        invalidateCache(userId, monitor.getId());

        return toResponse(monitor);
    }

    public MonitorResponse getMonitor(UUID monitorId, UUID userId) {
        return toResponse(findOwnedMonitor(monitorId, userId));
    }

    public List<MonitorResponse> getMonitorsByUser(UUID userId) {
        return monitorRepository.findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MonitorResponse updateMonitor(UUID monitorId, UUID userId, UpdateMonitorRequest request) {

        Monitor monitor = findOwnedMonitor(monitorId, userId);

        if (request.name() != null) monitor.setName(request.name().trim());

        if (request.url() != null) {
            MonitorValidator.validateUrl(request.url());
            monitor.setUrl(request.url().trim());
        }

        if (request.checkIntervalMin() != null) {
            User user = userRepository.findById(userId).orElseThrow();

            int minInterval = user.getPlan() == Plan.PRO ? PRO_MIN_INTERVAL : FREE_MIN_INTERVAL;

            if (request.checkIntervalMin() < minInterval) {
                throw new PlanLimitException(
                        "Minimum interval on " + user.getPlan().name().toLowerCase()
                                + " is " + minInterval + " min."
                );
            }

            MonitorValidator.validateInterval(request.checkIntervalMin());
            monitor.setCheckIntervalMin(request.checkIntervalMin());
        }

        if (request.httpMethod() != null) {
            MonitorValidator.validateHttpMethod(request.httpMethod());
            monitor.setHttpMethod(request.httpMethod().toUpperCase());
        }

        if (request.expectedStatus() != null) monitor.setExpectedStatus(request.expectedStatus());

        if (request.timeoutSeconds() != null) {
            MonitorValidator.validateTimeout(request.timeoutSeconds());
            monitor.setTimeoutSeconds(request.timeoutSeconds());
        }

        if (request.requestHeaders() != null) monitor.setRequestHeaders(request.requestHeaders());
        if (request.requestBody() != null) monitor.setRequestBody(request.requestBody());

        if (request.alertAfterFailures() != null) {
            MonitorValidator.validateAlertAfterFailures(request.alertAfterFailures());
            monitor.setAlertAfterFailures(request.alertAfterFailures());
        }

        if (request.expectedKeyword() != null) monitor.setExpectedKeyword(request.expectedKeyword());
        if (request.basicAuthUsername() != null) monitor.setBasicAuthUsername(request.basicAuthUsername());

        if (request.basicAuthPassword() != null && !request.basicAuthPassword().isBlank()) {
            monitor.setBasicAuthPasswordEnc(encryptionService.encrypt(request.basicAuthPassword()));
        }

        if (request.bearerToken() != null && !request.bearerToken().isBlank()) {
            monitor.setBearerTokenEnc(encryptionService.encrypt(request.bearerToken()));
        }

        monitor = monitorRepository.save(monitor);

        invalidateCache(userId, monitorId);

        return toResponse(monitor);
    }

    @Transactional
    public void deleteMonitor(UUID monitorId, UUID userId) {
        monitorRepository.delete(findOwnedMonitor(monitorId, userId));
        invalidateCache(userId, monitorId);
    }

    @Transactional
    public void pauseMonitor(UUID monitorId, UUID userId) {
        findOwnedMonitor(monitorId, userId);

        monitorRepository.updateIsActive(monitorId, false);
        monitorRepository.updateStatus(monitorId, MonitorStatus.PAUSED.getDbValue());

        invalidateCache(userId, monitorId);
    }

    @Transactional
    public void resumeMonitor(UUID monitorId, UUID userId) {
        findOwnedMonitor(monitorId, userId);

        monitorRepository.updateIsActive(monitorId, true);
        monitorRepository.updateStatus(monitorId, MonitorStatus.UNKNOWN.getDbValue());
        monitorRepository.updateConsecutiveFailures(monitorId, 0);
        monitorRepository.updateNextCheckAt(monitorId, OffsetDateTime.now());

        invalidateCache(userId, monitorId);
    }

    private Monitor findOwnedMonitor(UUID monitorId, UUID userId) {
        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found: " + monitorId));

        if (!monitor.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Monitor not found: " + monitorId);
        }

        return monitor;
    }

    private void validatePlanLimits(User user, UUID userId, int intervalMin) {

        int limit = user.getPlan() == Plan.PRO ? PRO_MONITOR_LIMIT : FREE_MONITOR_LIMIT;

        if (monitorRepository.countByUserId(userId) >= limit) {
            throw new PlanLimitException(
                    "Plan limit reached: " + limit
            );
        }

        int minInterval = user.getPlan() == Plan.PRO ? PRO_MIN_INTERVAL : FREE_MIN_INTERVAL;

        if (intervalMin < minInterval) {
            throw new PlanLimitException(
                    "Minimum interval is " + minInterval
            );
        }

        MonitorValidator.validateInterval(intervalMin);
    }

    private void validateMonitorFields(MonitorType type, String url, String httpMethod,
                                       Integer timeoutSeconds, Integer alertAfterFailures, String expectedKeyword) {

        if (type != MonitorType.HEARTBEAT) {
            MonitorValidator.validateUrl(url);
        }

        if (type == MonitorType.HTTP && httpMethod != null) {
            MonitorValidator.validateHttpMethod(httpMethod);
        }

        if (timeoutSeconds != null) {
            MonitorValidator.validateTimeout(timeoutSeconds);
        }

        if (alertAfterFailures != null) {
            MonitorValidator.validateAlertAfterFailures(alertAfterFailures);
        }

        if (type == MonitorType.KEYWORD) {
            MonitorValidator.validateKeyword(expectedKeyword);
        }
    }

    private void applyAuthFields(Monitor monitor, String basicAuthPassword, String bearerToken) {

        if (basicAuthPassword != null && !basicAuthPassword.isBlank()) {
            monitor.setBasicAuthPasswordEnc(encryptionService.encrypt(basicAuthPassword));
        }

        if (bearerToken != null && !bearerToken.isBlank()) {
            monitor.setBearerTokenEnc(encryptionService.encrypt(bearerToken));
        }
    }

    private void invalidateCache(UUID userId, UUID monitorId) {
        try {
            redisTemplate.delete("cache:dashboard:" + userId);
            redisTemplate.delete("cache:monitor:" + monitorId + ":status");
        } catch (Exception ignored) {}
    }

    public MonitorResponse mapToResponse(Monitor monitor) {
        return toResponse(monitor);
    }

    private MonitorResponse toResponse(Monitor monitor) {

        String status = Boolean.FALSE.equals(monitor.getIsActive())
                ? MonitorStatus.PAUSED.getDbValue()
                : monitor.getStatus();

        return new MonitorResponse(
                monitor.getId(),
                monitor.getName(),
                monitor.getType(),
                monitor.getUrl(),
                monitor.getPingKey(),
                monitor.getCheckIntervalMin(),
                monitor.getHttpMethod(),
                monitor.getExpectedStatus(),
                monitor.getTimeoutSeconds(),
                status,
                monitor.getIsActive(),
                monitor.getCreatedAt(),
                monitor.getRequestHeaders(),
                monitor.getRequestBody(),
                monitor.getSslDaysRemaining(),
                monitor.getSslExpiresAt(),
                monitor.getSslIssuer(),
                monitor.getLastCheckedAt(),
                monitor.getLastPingedAt(),
                monitor.getExpectedNextHeartbeatAt(),
                monitor.getLastResponseTimeMs(),
                monitor.getConsecutiveFailures() == null ? 0 : monitor.getConsecutiveFailures(),
                monitor.getAlertAfterFailures() == null ? 1 : monitor.getAlertAfterFailures(),
                monitor.getExpectedKeyword(),
                monitor.getBasicAuthUsername(),
                monitor.getBasicAuthPasswordEnc() != null && !monitor.getBasicAuthPasswordEnc().isBlank(),
                monitor.getBearerTokenEnc() != null && !monitor.getBearerTokenEnc().isBlank()
        );
    }
}