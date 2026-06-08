package akakash.backend.monitor;

import akakash.backend.common.dto.CreateMonitorRequest;
import akakash.backend.common.dto.MonitorResponse;
import akakash.backend.common.dto.UpdateMonitorRequest;
import akakash.backend.common.exception.PlanLimitException;
import akakash.backend.config.SecretEncryptionService;
import akakash.backend.user.Plan;
import akakash.backend.user.User;
import akakash.backend.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecretEncryptionService encryptionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private MonitorService monitorService;
    private UUID userId;
    private User freeUser;

    @BeforeEach
    void setUp() {
        monitorService = new MonitorService(monitorRepository, userRepository, encryptionService, redisTemplate);
        userId = UUID.randomUUID();
        freeUser = User.builder()
                .id(userId)
                .email("user@example.com")
                .plan(Plan.FREE)
                .build();
    }

    @Test
    void createMonitorAppliesDefaultsEncryptsAuthAndInvalidatesCache() {
        CreateMonitorRequest request = new CreateMonitorRequest(
                "  API  ",
                "http",
                " https://example.com/health ",
                2,
                null,
                null,
                null,
                "{\"X-Test\":\"true\"}",
                null,
                null,
                null,
                "basic-user",
                "basic-pass",
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(monitorRepository.countByUserId(userId)).thenReturn(0L);
        when(encryptionService.encrypt("basic-pass")).thenReturn("enc-pass");
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(invocation -> {
            Monitor monitor = invocation.getArgument(0);
            monitor.setId(UUID.randomUUID());
            return monitor;
        });

        MonitorResponse response = monitorService.createMonitor(userId, request);

        ArgumentCaptor<Monitor> monitorCaptor = ArgumentCaptor.forClass(Monitor.class);
        verify(monitorRepository).save(monitorCaptor.capture());
        Monitor saved = monitorCaptor.getValue();

        assertThat(saved.getName()).isEqualTo("API");
        assertThat(saved.getType()).isEqualTo("http");
        assertThat(saved.getUrl()).isEqualTo("https://example.com/health");
        assertThat(saved.getHttpMethod()).isEqualTo("GET");
        assertThat(saved.getExpectedStatus()).isEqualTo(200);
        assertThat(saved.getTimeoutSeconds()).isEqualTo(30);
        assertThat(saved.getAlertAfterFailures()).isEqualTo(1);
        assertThat(saved.getBasicAuthPasswordEnc()).isEqualTo("enc-pass");
        assertThat(response.hasBasicAuth()).isTrue();
        verify(redisTemplate).delete("cache:dashboard:" + userId);
        verify(redisTemplate).delete("cache:monitor:" + saved.getId() + ":status");
    }

    @Test
    void createHeartbeatMonitorGeneratesPingKeyAndAllowsMissingUrl() {
        CreateMonitorRequest request = new CreateMonitorRequest(
                "Cron job",
                "cron",
                null,
                2,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(monitorRepository.countByUserId(userId)).thenReturn(0L);
        when(monitorRepository.save(any(Monitor.class))).thenAnswer(invocation -> {
            Monitor monitor = invocation.getArgument(0);
            monitor.setId(UUID.randomUUID());
            return monitor;
        });

        MonitorResponse response = monitorService.createMonitor(userId, request);

        assertThat(response.type()).isEqualTo("cron");
        assertThat(response.pingKey()).isNotBlank();
        assertThat(response.url()).isNull();
    }

    @Test
    void createMonitorRejectsFreePlanCountLimit() {
        CreateMonitorRequest request = validHttpCreateRequest(2);

        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(monitorRepository.countByUserId(userId)).thenReturn(3L);

        assertThatThrownBy(() -> monitorService.createMonitor(userId, request))
                .isInstanceOf(PlanLimitException.class)
                .hasMessageContaining("Plan limit reached: 3");
    }

    @Test
    void createMonitorRejectsIntervalBelowPlanMinimum() {
        CreateMonitorRequest request = validHttpCreateRequest(1);

        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(monitorRepository.countByUserId(userId)).thenReturn(0L);

        assertThatThrownBy(() -> monitorService.createMonitor(userId, request))
                .isInstanceOf(PlanLimitException.class)
                .hasMessageContaining("Minimum interval is 2");
    }

    @Test
    void createKeywordMonitorRequiresKeyword() {
        CreateMonitorRequest request = new CreateMonitorRequest(
                "Keyword",
                "keyword",
                "https://example.com",
                2,
                "GET",
                200,
                10,
                null,
                null,
                1,
                " ",
                null,
                null,
                null
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(monitorRepository.countByUserId(userId)).thenReturn(0L);

        assertThatThrownBy(() -> monitorService.createMonitor(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("keyword");
    }

    @Test
    void updateMonitorAppliesValidatedFieldsAndEncryptsNewBearerToken() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = existingMonitor(monitorId, freeUser);
        UpdateMonitorRequest request = new UpdateMonitorRequest(
                "  API v2  ",
                "https://example.com/v2",
                3,
                "post",
                202,
                20,
                "{\"Accept\":\"application/json\"}",
                "{\"ok\":true}",
                2,
                null,
                null,
                null,
                "new-token"
        );

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));
        when(encryptionService.encrypt("new-token")).thenReturn("enc-token");
        when(monitorRepository.save(monitor)).thenReturn(monitor);

        MonitorResponse response = monitorService.updateMonitor(monitorId, userId, request);

        assertThat(response.name()).isEqualTo("API v2");
        assertThat(response.url()).isEqualTo("https://example.com/v2");
        assertThat(response.checkIntervalMin()).isEqualTo(3);
        assertThat(response.httpMethod()).isEqualTo("POST");
        assertThat(response.expectedStatus()).isEqualTo(202);
        assertThat(response.timeoutSeconds()).isEqualTo(20);
        assertThat(response.hasBearerToken()).isTrue();
        verify(redisTemplate).delete("cache:dashboard:" + userId);
        verify(redisTemplate).delete("cache:monitor:" + monitorId + ":status");
    }

    @Test
    void updateMonitorRejectsIntervalBelowFreePlanMinimum() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = existingMonitor(monitorId, freeUser);
        UpdateMonitorRequest request = new UpdateMonitorRequest(
                null, null, 1, null, null, null, null, null, null, null, null, null, null
        );

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(userRepository.findById(userId)).thenReturn(Optional.of(freeUser));

        assertThatThrownBy(() -> monitorService.updateMonitor(monitorId, userId, request))
                .isInstanceOf(PlanLimitException.class)
                .hasMessageContaining("Minimum interval on free is 2 min");
    }

    @Test
    void deleteMonitorDeletesOnlyOwnedMonitorAndInvalidatesCache() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = existingMonitor(monitorId, freeUser);

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        monitorService.deleteMonitor(monitorId, userId);

        verify(monitorRepository).delete(monitor);
        verify(redisTemplate).delete("cache:dashboard:" + userId);
        verify(redisTemplate).delete("cache:monitor:" + monitorId + ":status");
    }

    private CreateMonitorRequest validHttpCreateRequest(int intervalMin) {
        return new CreateMonitorRequest(
                "API",
                "http",
                "https://example.com",
                intervalMin,
                "GET",
                200,
                10,
                null,
                null,
                1,
                null,
                null,
                null,
                null
        );
    }

    private Monitor existingMonitor(UUID monitorId, User user) {
        return Monitor.builder()
                .id(monitorId)
                .user(user)
                .name("API")
                .type("http")
                .url("https://example.com")
                .checkIntervalMin(2)
                .httpMethod("GET")
                .expectedStatus(200)
                .timeoutSeconds(10)
                .status("up")
                .isActive(true)
                .consecutiveFailures(0)
                .alertAfterFailures(1)
                .build();
    }
}
