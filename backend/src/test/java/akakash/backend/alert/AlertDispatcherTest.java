package akakash.backend.alert;

import akakash.backend.incident.Incident;
import akakash.backend.monitor.Monitor;
import akakash.backend.notification.Notification;
import akakash.backend.notification.NotificationPreferencesService;
import akakash.backend.notification.NotificationRepository;
import akakash.backend.user.Plan;
import akakash.backend.user.User;
import akakash.backend.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertDispatcherTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationPreferencesService preferencesService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void dispatchCreatesNotificationAndQueuesAlertWhenPreferenceAllowsAndDedupIsNew() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        UUID incidentId = UUID.randomUUID();
        Monitor monitor = monitor(monitorId, userId);
        Incident incident = Incident.builder()
                .id(incidentId)
                .monitorId(monitorId)
                .status("open")
                .startedAt(OffsetDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(monitor.getUser()));
        when(preferencesService.shouldSendEmail(userId, "down")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("alert:sent:" + monitorId + ":down"), eq("1"), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.opsForList()).thenReturn(listOperations);

        new AlertDispatcher(notificationRepository, userRepository, preferencesService, redisTemplate, objectMapper)
                .dispatch(monitor, incident, "down");

        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(notificationCaptor.getValue().getTitle()).isEqualTo("API is DOWN");
        assertThat(notificationCaptor.getValue().getIsRead()).isFalse();

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(listOperations).rightPush(eq("queue:alerts"), payloadCaptor.capture());
        AlertJob job = objectMapper.readValue(payloadCaptor.getValue(), AlertJob.class);
        assertThat(job.monitorId()).isEqualTo(monitorId);
        assertThat(job.incidentId()).isEqualTo(incidentId);
        assertThat(job.alertType()).isEqualTo("down");
        assertThat(job.recipientEmail()).isEqualTo("user@example.com");
        assertThat(job.monitorName()).isEqualTo("API");
    }

    @Test
    void dispatchDoesNotQueueWhenPreferenceBlocksAlertType() {
        UUID userId = UUID.randomUUID();
        Monitor monitor = monitor(UUID.randomUUID(), userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(monitor.getUser()));
        when(preferencesService.shouldSendEmail(userId, "down")).thenReturn(false);

        new AlertDispatcher(notificationRepository, userRepository, preferencesService, redisTemplate, objectMapper)
                .dispatch(monitor, null, "down");

        verify(notificationRepository, never()).save(any());
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void dispatchPreventsDuplicateAlertsWithRedisDedupKey() {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = monitor(monitorId, userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(monitor.getUser()));
        when(preferencesService.shouldSendEmail(userId, "down")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("alert:sent:" + monitorId + ":down"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        new AlertDispatcher(notificationRepository, userRepository, preferencesService, redisTemplate, objectMapper)
                .dispatch(monitor, null, "down");

        verify(notificationRepository, never()).save(any());
        verify(redisTemplate, never()).opsForList();
    }

    @Test
    void dispatchSslExpiryPreventsDuplicateThresholdAlerts() {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = monitor(monitorId, userId);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("alert:sent:" + monitorId + ":ssl_expiry:30"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        new AlertDispatcher(notificationRepository, userRepository, preferencesService, redisTemplate, objectMapper)
                .dispatchSslExpiry(monitor, 30);

        verify(userRepository, never()).findById(userId);
        verify(notificationRepository, never()).save(any());
    }

    private Monitor monitor(UUID monitorId, UUID userId) {
        User user = User.builder()
                .id(userId)
                .email("user@example.com")
                .plan(Plan.FREE)
                .build();
        return Monitor.builder()
                .id(monitorId)
                .user(user)
                .name("API")
                .url("https://example.com")
                .build();
    }
}
