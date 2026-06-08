package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronCheckerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Test
    void evaluateReturnsDownWhenNoPingWasReceived() {
        UUID monitorId = UUID.randomUUID();
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(cronMonitor(monitorId, null, 5)));

        CheckResult result = new CronChecker(monitorRepository).evaluate(monitorId);

        assertThat(result.status()).isEqualTo("down");
        assertThat(result.errorMessage()).isEqualTo("No ping received yet");
    }

    @Test
    void evaluateReturnsDownWhenPingMissesGraceWindow() {
        UUID monitorId = UUID.randomUUID();
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(cronMonitor(
                monitorId,
                OffsetDateTime.now().minusMinutes(11),
                5
        )));

        CheckResult result = new CronChecker(monitorRepository).evaluate(monitorId);

        assertThat(result.status()).isEqualTo("down");
        assertThat(result.errorMessage()).contains("threshold: 10 min");
    }

    @Test
    void evaluateReturnsUpInsideGraceWindow() {
        UUID monitorId = UUID.randomUUID();
        when(monitorRepository.findById(monitorId)).thenReturn(Optional.of(cronMonitor(
                monitorId,
                OffsetDateTime.now().minusMinutes(3),
                5
        )));

        CheckResult result = new CronChecker(monitorRepository).evaluate(monitorId);

        assertThat(result.status()).isEqualTo("up");
        assertThat(result.responseTimeMs()).isGreaterThanOrEqualTo(0);
    }

    private Monitor cronMonitor(UUID monitorId, OffsetDateTime lastPingedAt, int intervalMin) {
        return Monitor.builder()
                .id(monitorId)
                .lastPingedAt(lastPingedAt)
                .checkIntervalMin(intervalMin)
                .build();
    }
}
