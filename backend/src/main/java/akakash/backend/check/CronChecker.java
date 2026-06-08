package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CronChecker {

    private final MonitorRepository monitorRepository;

    public CheckResult evaluate(UUID monitorId) {
        Monitor monitor = monitorRepository.findById(monitorId).orElseThrow();

        OffsetDateTime lastPingedAt = monitor.getLastPingedAt();
        if (lastPingedAt == null) {
            return new CheckResult(monitorId, "down", null, null, "No ping received yet");
        }

        long minutesSinceLastPing = ChronoUnit.MINUTES.between(lastPingedAt, OffsetDateTime.now());
        int graceMultiplier = 2;
        long threshold = monitor.getCheckIntervalMin() * graceMultiplier;

        if (minutesSinceLastPing >= threshold) {
            return new CheckResult(monitorId, "down", null, null, "Cron ping missed for " + minutesSinceLastPing + " minutes (threshold: " + threshold + " min)");
        }

        return new CheckResult(monitorId, "up", (int) (minutesSinceLastPing * 1000), null, null);
    }
}