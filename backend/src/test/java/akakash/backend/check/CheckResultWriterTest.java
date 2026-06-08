package akakash.backend.check;

import akakash.backend.incident.IncidentService;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

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
class CheckResultWriterTest {

    @Mock
    private MonitorCheckRepository checkRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private IncidentService incidentService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void writePersistsCheckUpdatesMonitorAndDelegatesIncidentHandling() {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = Monitor.builder()
                .id(monitorId)
                .user(User.builder().id(userId).build())
                .build();
        CheckResult result = new CheckResult(monitorId, "down", 120, 500, "boom");

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        new CheckResultWriter(checkRepository, monitorRepository, incidentService, redisTemplate).write(result);

        ArgumentCaptor<MonitorCheck> checkCaptor = ArgumentCaptor.forClass(MonitorCheck.class);
        verify(checkRepository).saveAndFlush(checkCaptor.capture());
        MonitorCheck savedCheck = checkCaptor.getValue();
        assertThat(savedCheck.getMonitorId()).isEqualTo(monitorId);
        assertThat(savedCheck.getStatus()).isEqualTo("down");
        assertThat(savedCheck.getResponseTimeMs()).isEqualTo(120);
        assertThat(savedCheck.getHttpStatusCode()).isEqualTo(500);
        assertThat(savedCheck.getErrorMessage()).isEqualTo("boom");

        verify(monitorRepository).updateLastCheckedAt(eq(monitorId), any(OffsetDateTime.class));
        verify(incidentService).handleCheckResult(monitor, result);
        verify(redisTemplate).delete("cache:monitor:" + monitorId + ":status");
    }

    @Test
    void writeSkipsPersistenceWhenMonitorDoesNotExist() {
        UUID monitorId = UUID.randomUUID();
        CheckResult result = new CheckResult(monitorId, "down", null, null, "missing");
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.empty());

        new CheckResultWriter(checkRepository, monitorRepository, incidentService, redisTemplate).write(result);

        verify(checkRepository, never()).saveAndFlush(any());
        verify(incidentService, never()).handleCheckResult(any(), any());
    }
}
