package akakash.backend.check;

import akakash.backend.alert.AlertDispatcher;
import akakash.backend.alert.SslAlertSent;
import akakash.backend.alert.SslAlertSentRepository;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SslExpiryScanner {

    private static final int[] THRESHOLDS = {30, 15, 7, 3, 1};

    private final MonitorRepository monitorRepository;
    private final AlertDispatcher alertDispatcher;
    private final SslAlertSentRepository sslAlertSentRepository;
    private final SslChecker sslChecker;

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void scanSslExpiry() {
        log.info("Starting SSL expiry scan");

        List<Monitor> sslMonitors = monitorRepository.findByTypeAndIsActiveTrue("ssl");

        for (Monitor monitor : sslMonitors) {
            try {
                sslChecker.check(monitor.getId());

                monitorRepository.findById(monitor.getId()).ifPresent(updatedMonitor -> {
                    Integer daysRemaining = updatedMonitor.getSslDaysRemaining();
                    if (daysRemaining == null) return;

                    for (int threshold : THRESHOLDS) {
                        if (daysRemaining <= threshold && daysRemaining > 0) {
                            if (!sslAlertSentRepository.existsByMonitorIdAndThresholdDays(monitor.getId(), threshold)) {
                                alertDispatcher.dispatchSslExpiry(updatedMonitor, threshold);
                                sslAlertSentRepository.save(SslAlertSent.builder()
                                        .monitorId(monitor.getId())
                                        .thresholdDays(threshold)
                                        .build());
                                log.info("SSL expiry alert at {} days for monitor: {}", threshold, monitor.getName());
                            }
                        }
                    }
                });
            } catch (Exception e) {
                log.error("Error checking SSL for monitor {}: {}", monitor.getName(), e.getMessage());
            }
        }

        log.info("SSL expiry scan completed");
    }
}
