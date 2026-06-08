package akakash.backend.alert;

import akakash.backend.incident.IncidentRepository;
import akakash.backend.monitor.MonitorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertWorkerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AlertLogRepository alertLogRepository;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void processQueueSendsEmailAndSavesSentLog() throws Exception {
        AlertJob job = new AlertJob(UUID.randomUUID(), UUID.randomUUID(), "down", "user@example.com", "API");
        AtomicReference<AlertWorker> workerRef = new AtomicReference<>();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop(eq("queue:alerts"), any(Duration.class)))
                .thenReturn(objectMapper.writeValueAsString(job));
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(Map.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", "email_123")));
        doAnswer(invocation -> {
            workerRef.get().stop();
            return invocation.getArgument(0);
        }).when(alertLogRepository).save(any(AlertLog.class));

        AlertWorker worker = worker();
        workerRef.set(worker);

        worker.processQueue();

        ArgumentCaptor<AlertLog> logCaptor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAlertType()).isEqualTo("down");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("sent");
        assertThat(logCaptor.getValue().getSentTo()).isEqualTo("user@example.com");
    }

    @Test
    void processQueueSavesFailedLogWhenEmailSendFails() throws Exception {
        AlertJob job = new AlertJob(UUID.randomUUID(), UUID.randomUUID(), "down", "user@example.com", "API");
        AtomicReference<AlertWorker> workerRef = new AtomicReference<>();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop(eq("queue:alerts"), any(Duration.class)))
                .thenReturn(objectMapper.writeValueAsString(job));
        when(restTemplate.postForEntity(eq("https://api.resend.com/emails"), any(), eq(Map.class)))
                .thenThrow(new RuntimeException("resend down"));
        doAnswer(invocation -> {
            workerRef.get().stop();
            return invocation.getArgument(0);
        }).when(alertLogRepository).save(any(AlertLog.class));

        AlertWorker worker = worker();
        workerRef.set(worker);

        worker.processQueue();

        ArgumentCaptor<AlertLog> logCaptor = ArgumentCaptor.forClass(AlertLog.class);
        verify(alertLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("failed");
        assertThat(logCaptor.getValue().getErrorMessage()).contains("resend down");
    }

    @Test
    void processQueueIgnoresInvalidJsonWithoutSavingLog() {
        AtomicReference<AlertWorker> workerRef = new AtomicReference<>();

        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop(eq("queue:alerts"), any(Duration.class)))
                .thenAnswer(invocation -> {
                    workerRef.get().stop();
                    return "{bad-json";
                });

        AlertWorker worker = worker();
        workerRef.set(worker);

        worker.processQueue();

        verify(alertLogRepository, never()).save(any());
    }

    private AlertWorker worker() {
        AlertWorker worker = new AlertWorker(redisTemplate, monitorRepository, incidentRepository, alertLogRepository, objectMapper);
        ReflectionTestUtils.setField(worker, "resendApiKey", "test-key");
        ReflectionTestUtils.setField(worker, "fromEmail", "alerts@example.com");
        ReflectionTestUtils.setField(worker, "restTemplate", restTemplate);
        return worker;
    }
}
