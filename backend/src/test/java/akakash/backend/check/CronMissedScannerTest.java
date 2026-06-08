package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CronMissedScannerTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private CheckResultWriter checkResultWriter;

    @Test
    void scanMissedCronsWritesDownResultForLateHeartbeat() {
        UUID monitorId = UUID.randomUUID();
        Monitor monitor = Monitor.builder()
                .id(monitorId)
                .name("Nightly job")
                .type("cron")
                .status("up")
                .lastPingedAt(OffsetDateTime.now().minusMinutes(20))
                .expectedNextHeartbeatAt(OffsetDateTime.now().minusMinutes(10))
                .checkIntervalMin(5)
                .build();
        when(monitorRepository.findByTypeAndIsActiveTrue("cron")).thenReturn(List.of(monitor));

        new CronMissedScanner(monitorRepository, checkResultWriter).scanMissedCrons();

        ArgumentCaptor<CheckResult> resultCaptor = ArgumentCaptor.forClass(CheckResult.class);
        verify(checkResultWriter).write(resultCaptor.capture());
        CheckResult result = resultCaptor.getValue();
        assertThat(result.monitorId()).isEqualTo(monitorId);
        assertThat(result.status()).isEqualTo("down");
        assertThat(result.errorMessage()).contains("Heartbeat missed");
    }

    @Test
    void scanMissedCronsSkipsAlreadyDownMonitor() {
        Monitor monitor = Monitor.builder()
                .id(UUID.randomUUID())
                .name("Nightly job")
                .type("cron")
                .status("down")
                .lastPingedAt(OffsetDateTime.now().minusMinutes(20))
                .expectedNextHeartbeatAt(OffsetDateTime.now().minusMinutes(10))
                .checkIntervalMin(5)
                .build();
        when(monitorRepository.findByTypeAndIsActiveTrue("cron")).thenReturn(List.of(monitor));

        new CronMissedScanner(monitorRepository, checkResultWriter).scanMissedCrons();

        verify(checkResultWriter, never()).write(org.mockito.ArgumentMatchers.any());
    }
}
