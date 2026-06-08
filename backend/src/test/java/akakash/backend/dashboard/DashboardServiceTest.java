package akakash.backend.dashboard;

import akakash.backend.check.MonitorCheckRepository;
import akakash.backend.incident.Incident;
import akakash.backend.incident.IncidentRepository;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private MonitorCheckRepository checkRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Test
    void getSummaryAggregatesMonitorCountsSslWarningsAndOpenIncidents() {
        UUID userId = UUID.randomUUID();
        UUID monitorOne = UUID.randomUUID();
        UUID monitorTwo = UUID.randomUUID();
        UUID unrelatedMonitor = UUID.randomUUID();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:dashboard:" + userId)).thenReturn(null);
        when(monitorRepository.findByUserId(userId)).thenReturn(List.of(
                monitor(monitorOne, true, "up", 10),
                monitor(monitorTwo, true, "down", 40),
                monitor(UUID.randomUUID(), false, "degraded", 5)
        ));
        when(incidentRepository.findAllOpenIncidents()).thenReturn(List.of(
                Incident.builder().monitorId(monitorTwo).status("open").build(),
                Incident.builder().monitorId(unrelatedMonitor).status("open").build()
        ));

        DashboardSummary summary = new DashboardService(
                monitorRepository,
                incidentRepository,
                checkRepository,
                redisTemplate,
                new ObjectMapper()
        ).getSummary(userId);

        assertThat(summary.totalMonitors()).isEqualTo(3);
        assertThat(summary.activeMonitors()).isEqualTo(2);
        assertThat(summary.upMonitors()).isEqualTo(1);
        assertThat(summary.downMonitors()).isEqualTo(1);
        assertThat(summary.unknownMonitors()).isEqualTo(1);
        assertThat(summary.sslWarnings()).isEqualTo(2);
        assertThat(summary.openIncidents()).isEqualTo(1);
        verify(valueOperations).set(eq("cache:dashboard:" + userId), eq(summary), any(java.time.Duration.class));
    }

    @Test
    void getSummaryReturnsCachedSummaryWhenPresent() {
        UUID userId = UUID.randomUUID();
        DashboardSummary cached = new DashboardSummary(1, 1, 1, 0, 0, 0, 0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:dashboard:" + userId)).thenReturn(cached);

        DashboardSummary summary = new DashboardService(
                monitorRepository,
                incidentRepository,
                checkRepository,
                redisTemplate,
                new ObjectMapper()
        ).getSummary(userId);

        assertThat(summary).isSameAs(cached);
    }

    private Monitor monitor(UUID id, boolean active, String status, Integer sslDaysRemaining) {
        return Monitor.builder()
                .id(id)
                .isActive(active)
                .status(status)
                .sslDaysRemaining(sslDaysRemaining)
                .build();
    }
}
