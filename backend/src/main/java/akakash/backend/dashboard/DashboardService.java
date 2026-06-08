package akakash.backend.dashboard;

import akakash.backend.check.MonitorCheckRepository;
import akakash.backend.common.dto.DashboardActivityResponse;
import akakash.backend.common.dto.IncidentResponse;
import akakash.backend.common.dto.MonitorCheckResponse;
import akakash.backend.incident.IncidentRepository;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MonitorRepository monitorRepository;
    private final IncidentRepository incidentRepository;
    private final MonitorCheckRepository checkRepository;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public DashboardSummary getSummary(UUID userId) {
        String key = "cache:dashboard:" + userId;
        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                if (cached instanceof DashboardSummary summary) {
                    return summary;
                }
                return objectMapper.convertValue(cached, DashboardSummary.class);
            }
        } catch (Exception ignored) {}

        List<Monitor> monitors = monitorRepository.findByUserId(userId);
        List<UUID> monitorIds = monitors.stream().map(Monitor::getId).toList();

        long total = monitors.size();
        long active = monitors.stream().filter(Monitor::getIsActive).count();
        long up = monitors.stream().filter(m -> "up".equals(m.getStatus())).count();
        long down = monitors.stream().filter(m -> "down".equals(m.getStatus())).count();
        long unknown = monitors.stream().filter(m -> "unknown".equals(m.getStatus()) || "degraded".equals(m.getStatus())).count();
        long sslWarnings = monitors.stream()
                .filter(m -> m.getSslDaysRemaining() != null && m.getSslDaysRemaining() <= 30)
                .count();
        long openIncidents = monitorIds.isEmpty() ? 0 : incidentRepository.findAllOpenIncidents().stream()
                .filter(i -> monitorIds.contains(i.getMonitorId()))
                .count();

        DashboardSummary summary = new DashboardSummary(total, active, up, down, unknown, sslWarnings, openIncidents);

        try {
            redisTemplate.opsForValue().set(key, summary, java.time.Duration.ofSeconds(30));
        } catch (Exception ignored) {}

        return summary;
    }

    public DashboardActivityResponse getActivity(UUID userId, int limit) {
        List<Monitor> monitors = monitorRepository.findByUserId(userId);
        List<UUID> monitorIds = monitors.stream().map(Monitor::getId).toList();

        if (monitorIds.isEmpty()) {
            return new DashboardActivityResponse(List.of(), List.of(), List.of());
        }

        PageRequest page = PageRequest.of(0, limit);
        List<MonitorCheckResponse> recentChecks = checkRepository
                .findByMonitorIdInOrderByCheckedAtDesc(monitorIds, page)
                .stream().map(MonitorCheckResponse::fromEntity).toList();

        List<IncidentResponse> recentIncidents = incidentRepository
                .findByMonitorIdInOrderByStartedAtDesc(monitorIds, page)
                .stream().map(IncidentResponse::fromEntity).toList();

        List<MonitorCheckResponse> recentFailures = checkRepository
                .findFailuresByMonitorIdIn(monitorIds, page)
                .stream().map(MonitorCheckResponse::fromEntity).toList();

        return new DashboardActivityResponse(recentChecks, recentIncidents, recentFailures);
    }
}
