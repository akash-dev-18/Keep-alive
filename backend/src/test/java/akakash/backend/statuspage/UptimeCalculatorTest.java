package akakash.backend.statuspage;

import akakash.backend.check.MonitorCheck;
import akakash.backend.check.MonitorCheckRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UptimeCalculatorTest {

    @Mock
    private MonitorCheckRepository checkRepository;

    @Test
    void calculateUptimePercentReturnsOneHundredWhenNoChecksExist() {
        UUID monitorId = UUID.randomUUID();
        when(checkRepository.findByMonitorIdAndCheckedAtBetween(eq(monitorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of());

        double uptime = new UptimeCalculator(checkRepository).calculateUptimePercent(monitorId, 7);

        assertThat(uptime).isEqualTo(100.0);
    }

    @Test
    void calculateUptimePercentCountsUpAndDegradedAsAvailable() {
        UUID monitorId = UUID.randomUUID();
        when(checkRepository.findByMonitorIdAndCheckedAtBetween(eq(monitorId), any(OffsetDateTime.class), any(OffsetDateTime.class)))
                .thenReturn(List.of(
                        check("up"),
                        check("degraded"),
                        check("down")
                ));

        double uptime = new UptimeCalculator(checkRepository).calculateUptimePercent(monitorId, 30);

        assertThat(uptime).isEqualTo(66.67);
    }

    private MonitorCheck check(String status) {
        return MonitorCheck.builder()
                .monitorId(UUID.randomUUID())
                .status(status)
                .checkedAt(OffsetDateTime.now())
                .build();
    }
}
