package akakash.backend.incident;

import akakash.backend.alert.AlertDispatcher;
import akakash.backend.check.CheckResult;
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
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private AlertDispatcher alertDispatcher;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void handleCheckResultCreatesIncidentAndDispatchesDownAlertAfterThreshold() {
        UUID userId = UUID.randomUUID();
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = monitor(monitorId, userId, "up", 0, 1);
        CheckResult result = new CheckResult(monitorId, "down", 250, 500, "timeout");

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(incidentRepository.findOpenByMonitorId(monitorId)).thenReturn(Optional.empty());
        when(incidentRepository.saveAndFlush(any(Incident.class))).thenAnswer(invocation -> {
            Incident incident = invocation.getArgument(0);
            incident.setId(UUID.randomUUID());
            return incident;
        });

        new IncidentService(incidentRepository, monitorRepository, alertDispatcher, redisTemplate)
                .handleCheckResult(monitor, result);

        verify(monitorRepository).updateLastResponseTimeMs(monitorId, 250);
        verify(monitorRepository).updateConsecutiveFailures(monitorId, 1);
        verify(monitorRepository).updateStatus(monitorId, "down");

        ArgumentCaptor<Incident> incidentCaptor = ArgumentCaptor.forClass(Incident.class);
        verify(incidentRepository).saveAndFlush(incidentCaptor.capture());
        assertThat(incidentCaptor.getValue().getStatus()).isEqualTo("open");
        assertThat(incidentCaptor.getValue().getCause()).isEqualTo("timeout");
        verify(alertDispatcher).dispatch(eq(monitor), eq(incidentCaptor.getValue()), eq("down"));
        verify(redisTemplate).delete("cache:dashboard:" + userId);
        verify(redisTemplate).delete("cache:monitor:" + monitorId + ":status");
    }

    @Test
    void handleCheckResultDoesNotCreateIncidentBeforeFailureThreshold() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = monitor(monitorId, UUID.randomUUID(), "up", 0, 2);
        CheckResult result = new CheckResult(monitorId, "down", null, null, "timeout");

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));

        new IncidentService(incidentRepository, monitorRepository, alertDispatcher, redisTemplate)
                .handleCheckResult(monitor, result);

        verify(incidentRepository, never()).saveAndFlush(any());
        verify(alertDispatcher, never()).dispatch(any(), any(), any());
    }

    @Test
    void handleCheckResultResolvesOpenIncidentAndDispatchesRecovery() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = monitor(monitorId, UUID.randomUUID(), "down", 2, 1);
        Incident openIncident = Incident.builder()
                .id(UUID.randomUUID())
                .monitorId(monitorId)
                .status("open")
                .startedAt(OffsetDateTime.now().minusMinutes(5))
                .build();
        CheckResult result = new CheckResult(monitorId, "up", 100, 200, null);

        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(monitor));
        when(incidentRepository.findOpenByMonitorId(monitorId)).thenReturn(Optional.of(openIncident));
        when(incidentRepository.saveAndFlush(openIncident)).thenReturn(openIncident);

        new IncidentService(incidentRepository, monitorRepository, alertDispatcher, redisTemplate)
                .handleCheckResult(monitor, result);

        verify(monitorRepository).updateConsecutiveFailures(monitorId, 0);
        verify(monitorRepository).updateStatus(monitorId, "up");
        assertThat(openIncident.getStatus()).isEqualTo("resolved");
        assertThat(openIncident.getResolvedAt()).isNotNull();
        assertThat(openIncident.getDurationSeconds()).isPositive();
        verify(alertDispatcher).dispatch(monitor, openIncident, "recovery");
    }

    private Monitor monitor(UUID monitorId, UUID userId, String status, int consecutiveFailures, int alertAfterFailures) {
        return Monitor.builder()
                .id(monitorId)
                .user(User.builder().id(userId).build())
                .status(status)
                .consecutiveFailures(consecutiveFailures)
                .alertAfterFailures(alertAfterFailures)
                .build();
    }
}
