package akakash.backend.check;

import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CronMissedScanner {

    private final MonitorRepository monitorRepository;
    private final CheckResultWriter checkResultWriter;

    @Scheduled(fixedDelay = 60_000)
    public void scanMissedCrons() {
        log.debug("Starting heartbeat missed scan");

        List<Monitor> cronMonitors = monitorRepository.findByTypeAndIsActiveTrue("cron");

        for (Monitor monitor : cronMonitors) {
            if (monitor.getLastPingedAt() == null) {
                continue;
            }

            OffsetDateTime expectedPingTime = monitor.getExpectedNextHeartbeatAt() != null
                    ? monitor.getExpectedNextHeartbeatAt()
                    : monitor.getLastPingedAt().plusMinutes(monitor.getCheckIntervalMin());

            if (OffsetDateTime.now().isAfter(expectedPingTime)) {
                if (!"down".equals(monitor.getStatus())) {
                    long minutesSince = ChronoUnit.MINUTES.between(monitor.getLastPingedAt(), OffsetDateTime.now());
                    CheckResult result = new CheckResult(
                            monitor.getId(),
                            "down",
                            null,
                            null,
                            "Heartbeat missed for " + minutesSince + " minutes"
                    );
                    checkResultWriter.write(result);
                    log.info("Heartbeat missed check written for monitor: {}", monitor.getName());
                }
            }
        }

        log.debug("Heartbeat missed scan completed");
    }
}
