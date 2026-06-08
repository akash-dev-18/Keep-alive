package akakash.backend.check;

import akakash.backend.incident.IncidentService;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckResultWriter {

    private final MonitorCheckRepository checkRepository;
    private final MonitorRepository monitorRepository;
    private final IncidentService incidentService;
    private final RedisTemplate<String, Object> redisTemplate;

    public void write(CheckResult result) {

        log.info("Writing check result: monitor={}, status={}, responseTime={}ms",
                result.monitorId(),
                result.status(),
                result.responseTimeMs());

        MonitorCheck check = MonitorCheck.builder()
                .monitorId(result.monitorId())
                .status(result.status())
                .responseTimeMs(result.responseTimeMs())
                .httpStatusCode(result.httpStatusCode())
                .errorMessage(result.errorMessage())
                .build();

        Monitor monitor = monitorRepository.findById(result.monitorId()).orElse(null);
        if (monitor == null) {
            log.warn("Monitor not found: {}", result.monitorId());
            return;
        }

        checkRepository.saveAndFlush(check);

        monitorRepository.updateLastCheckedAt(
                result.monitorId(),
                OffsetDateTime.now()
        );

        try {
            incidentService.handleCheckResult(monitor, result);
        } catch (Exception e) {
            log.error("Incident handling failed for {}", result.monitorId(), e);
        }

        try {
            redisTemplate.delete("cache:monitor:" + result.monitorId() + ":status");
        } catch (Exception e) {
            log.warn("Cache delete failed for {}", result.monitorId(), e);
        }
    }
    }
