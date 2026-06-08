package akakash.backend.check;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
@Component
@RequiredArgsConstructor
@Slf4j
public class CheckRetentionJob {

    private final MonitorCheckRepository monitorCheckRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeOldChecks() {

        log.info("Starting check retention cleanup");

        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(90);

        try {
            monitorCheckRepository.deleteByCheckedAtBefore(cutoff);
            log.info("Deleted checks older than 90 days");
        } catch (Exception e) {
            log.error("Error during check retention cleanup", e);
        } finally {
            log.info("Check retention cleanup completed");
        }
    }
}