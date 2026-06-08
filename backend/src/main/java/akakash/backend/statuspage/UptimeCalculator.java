package akakash.backend.statuspage;

import akakash.backend.check.MonitorCheck;
import akakash.backend.check.MonitorCheckRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UptimeCalculator {

    private final MonitorCheckRepository checkRepository;

    public double calculateUptimePercent(UUID monitorId, int days) {
        OffsetDateTime from = OffsetDateTime.now().minusDays(days);
        List<MonitorCheck> checks = checkRepository.findByMonitorIdAndCheckedAtBetween(
                monitorId, from, OffsetDateTime.now());

        if (checks.isEmpty()) {
            return 100.0;
        }

        long upCount = checks.stream().filter(c -> "up".equals(c.getStatus()) || "degraded".equals(c.getStatus())).count();
        return Math.round((upCount * 10000.0 / checks.size())) / 100.0;
    }
}
