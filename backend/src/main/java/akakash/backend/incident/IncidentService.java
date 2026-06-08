package akakash.backend.incident;

import akakash.backend.alert.AlertDispatcher;
import akakash.backend.check.CheckResult;
import akakash.backend.common.enums.MonitorStatus;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final MonitorRepository monitorRepository;
    private final AlertDispatcher alertDispatcher;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Transactional
    public void handleCheckResult(Monitor monitor, CheckResult result) {

        String previousStatus = monitorRepository.findById(monitor.getId())
                .map(Monitor::getStatus)
                .orElse("unknown");

        String newStatus = result.status();

        if (result.responseTimeMs() != null) {
            monitorRepository.updateLastResponseTimeMs(monitor.getId(), result.responseTimeMs());
        }

        int consecutiveFailures = monitor.getConsecutiveFailures() != null
                ? monitor.getConsecutiveFailures()
                : 0;

        if ("down".equals(newStatus)) {
            consecutiveFailures++;
            monitorRepository.updateConsecutiveFailures(monitor.getId(), consecutiveFailures);
        } else if (consecutiveFailures > 0) {
            consecutiveFailures = 0;
            monitorRepository.updateConsecutiveFailures(monitor.getId(), 0);
        }

        if ("down".equals(newStatus)) {
            monitorRepository.updateStatus(monitor.getId(), MonitorStatus.DOWN.getDbValue());
        } else if ("degraded".equals(newStatus)) {
            monitorRepository.updateStatus(monitor.getId(), MonitorStatus.DEGRADED.getDbValue());
        } else if ("up".equals(newStatus)) {
            monitorRepository.updateStatus(monitor.getId(), MonitorStatus.UP.getDbValue());
        }

        int alertThreshold = monitor.getAlertAfterFailures() != null
                ? monitor.getAlertAfterFailures()
                : 1;

        boolean shouldAlert = alertThreshold == 0 || consecutiveFailures >= alertThreshold;

        if ("down".equals(newStatus) && shouldAlert) {

            Incident open = incidentRepository.findOpenByMonitorId(monitor.getId()).orElse(null);

            if (open == null) {

                try {
                    // re-check inside transaction safety window
                    Incident secondCheck = incidentRepository.findOpenByMonitorId(monitor.getId()).orElse(null);

                    if (secondCheck == null) {
                        Incident incident = Incident.builder()
                                .monitorId(monitor.getId())
                                .status("open")
                                .cause(result.errorMessage())
                                .startedAt(OffsetDateTime.now())
                                .build();

                        incidentRepository.saveAndFlush(incident);
                        alertDispatcher.dispatch(monitor, incident, "down");
                    }

                } catch (Exception e) {

                }
            }
        } else if ("up".equals(newStatus)
                && ("down".equals(previousStatus) || "degraded".equals(previousStatus))) {

            Incident openIncident = incidentRepository.findOpenByMonitorId(monitor.getId())
                    .orElse(null);
            if (openIncident != null) {
                OffsetDateTime now = OffsetDateTime.now();

                openIncident.setStatus("resolved");
                openIncident.setResolvedAt(now);
                openIncident.setDurationSeconds(
                        (int) ChronoUnit.SECONDS.between(openIncident.getStartedAt(), now)
                );

                incidentRepository.saveAndFlush(openIncident);
                alertDispatcher.dispatch(monitor, openIncident, "recovery");
            }
        }

        invalidateDashboardCache(monitor);
    }

    public List<Incident> getIncidentsByMonitor(UUID monitorId) {
        return incidentRepository.findByMonitorIdOrderByStartedAtDesc(monitorId);
    }

    public List<Incident> getIncidentsForUser(List<UUID> monitorIds, int limit) {
        return incidentRepository.findByMonitorIdInOrderByStartedAtDesc(
                monitorIds,
                org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    private void invalidateDashboardCache(Monitor monitor) {
        try {
            redisTemplate.delete("cache:dashboard:" + monitor.getUser().getId());
            redisTemplate.delete("cache:monitor:" + monitor.getId() + ":status");
        } catch (Exception ignored) {}
    }
}