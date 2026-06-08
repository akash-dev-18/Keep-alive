package akakash.backend.statuspage;

import akakash.backend.common.dto.IncidentResponse;
import akakash.backend.common.dto.MonitorResponse;
import akakash.backend.common.dto.StatusPageResponse;
import akakash.backend.incident.IncidentRepository;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.monitor.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/status")
@RequiredArgsConstructor
@Slf4j
public class PublicStatusPageController {

    private final StatusPageService statusPageService;
    private final MonitorRepository monitorRepository;
    private final MonitorService monitorService;
    private final IncidentRepository incidentRepository;
    private final UptimeCalculator uptimeCalculator;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> getPublicStatusPage(@PathVariable String slug) {
        String key = "cache:statuspage:" + slug;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof Map) {
                return ResponseEntity.ok((Map<String, Object>) cached);
            }
        } catch (Exception e) {
            log.error("Failed to read status page cache for slug {}: {}", slug, e.getMessage());
        }

        StatusPage statusPage = statusPageService.getPublicStatusPage(slug);
        if (!statusPage.getIsPublic()) {
            return ResponseEntity.notFound().build();
        }

        List<Monitor> monitors = monitorRepository.findAllById(Arrays.asList(statusPage.getMonitorIds()));
        List<MonitorResponse> monitorResponses = monitors.stream()
                .map(monitorService::mapToResponse)
                .toList();

        Map<UUID, Double> uptime = new HashMap<>();
        for (Monitor m : monitors) {
            uptime.put(m.getId(), uptimeCalculator.calculateUptimePercent(m.getId(), 30));
        }

        List<UUID> monitorIds = monitors.stream().map(Monitor::getId).toList();
        List<IncidentResponse> incidents = monitorIds.isEmpty() ? List.of()
                : incidentRepository.findByMonitorIdInOrderByStartedAtDesc(monitorIds, PageRequest.of(0, 20))
                        .stream().map(IncidentResponse::fromEntity).toList();

        Map<String, Object> response = new HashMap<>();
        response.put("page", StatusPageResponse.fromEntity(statusPage));
        response.put("monitors", monitorResponses);
        response.put("uptime", uptime);
        response.put("incidents", incidents);

        try {
            redisTemplate.opsForValue().set(key, response, java.time.Duration.ofSeconds(60));
        } catch (Exception e) {
            log.error("Failed to write status page cache for slug {}: {}", slug, e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
