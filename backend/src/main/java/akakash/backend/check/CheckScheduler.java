package akakash.backend.check;

import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckScheduler {

    private final MonitorRepository monitorRepository;
    private final CheckEnqueuer checkEnqueuer;

    @Scheduled(fixedDelay = 10_000)
    @Transactional
    public void scheduleDueChecks() {
        var dueMonitors = monitorRepository.findDueForCheck(OffsetDateTime.now(), PageRequest.of(0, 200));

        if (dueMonitors.isEmpty()) return;
        log.info("Due checks: {}", dueMonitors.size());
        for (var monitor : dueMonitors) {
            try {
                monitorRepository.updateNextCheckAt(
                        monitor.getId(),
                        OffsetDateTime.now().plusMinutes(monitor.getCheckIntervalMin())
                );

                checkEnqueuer.enqueueOne(monitor);

            } catch (Exception e) {
                log.error("Failed enqueue for monitor {}. Reason: {}", monitor.getId(), e.getMessage(), e);
            }
        }
    }
}