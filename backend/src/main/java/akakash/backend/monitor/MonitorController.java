package akakash.backend.monitor;

import akakash.backend.check.MonitorCheck;
import akakash.backend.check.MonitorCheckRepository;
import akakash.backend.common.dto.CreateMonitorRequest;
import akakash.backend.common.dto.MonitorCheckResponse;
import akakash.backend.common.dto.IncidentResponse;
import akakash.backend.common.dto.MonitorResponse;
import akakash.backend.common.dto.UpdateMonitorRequest;
import akakash.backend.incident.Incident;
import akakash.backend.incident.IncidentRepository;
import akakash.backend.user.User;
import akakash.backend.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/monitors")
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorService monitorService;
    private final UserService userService;
    private final MonitorCheckRepository monitorCheckRepository;
    private final IncidentRepository incidentRepository;

    @PostMapping
    public ResponseEntity<MonitorResponse> createMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateMonitorRequest request) {
        UUID userId = getUserIdFromJwt(jwt);
        MonitorResponse response = monitorService.createMonitor(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MonitorResponse>> getMonitors(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserIdFromJwt(jwt);
        List<MonitorResponse> monitors = monitorService.getMonitorsByUser(userId);
        return ResponseEntity.ok(monitors);
    }

    @GetMapping("/{monitorId}")
    public ResponseEntity<MonitorResponse> getMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId) {
        UUID userId = getUserIdFromJwt(jwt);
        MonitorResponse monitor = monitorService.getMonitor(monitorId, userId);
        return ResponseEntity.ok(monitor);
    }

    @PutMapping("/{monitorId}")
    public ResponseEntity<MonitorResponse> updateMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId,
            @Valid @RequestBody UpdateMonitorRequest request) {
        UUID userId = getUserIdFromJwt(jwt);
        MonitorResponse response = monitorService.updateMonitor(monitorId, userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{monitorId}")
    public ResponseEntity<Void> deleteMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId) {
        UUID userId = getUserIdFromJwt(jwt);
        monitorService.deleteMonitor(monitorId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{monitorId}/pause")
    public ResponseEntity<Void> pauseMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId) {
        UUID userId = getUserIdFromJwt(jwt);
        monitorService.pauseMonitor(monitorId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{monitorId}/resume")
    public ResponseEntity<Void> resumeMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId) {
        UUID userId = getUserIdFromJwt(jwt);
        monitorService.resumeMonitor(monitorId, userId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{monitorId}/checks")
    public ResponseEntity<List<MonitorCheckResponse>> getMonitorChecks(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId,
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = getUserIdFromJwt(jwt);
        verifyMonitorOwnership(monitorId, userId);
        int cappedLimit = Math.min(Math.max(limit, 1), 200);
        List<MonitorCheck> checks = monitorCheckRepository.findByMonitorIdOrderByCheckedAtDesc(
                monitorId, PageRequest.of(0, cappedLimit));
        List<MonitorCheckResponse> response = checks.stream()
                .map(MonitorCheckResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{monitorId}/incidents")
    public ResponseEntity<List<IncidentResponse>> getMonitorIncidents(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID monitorId) {
        UUID userId = getUserIdFromJwt(jwt);
        verifyMonitorOwnership(monitorId, userId);
        List<Incident> incidents = incidentRepository.findByMonitorIdOrderByStartedAtDesc(monitorId);
        List<IncidentResponse> response = incidents.stream()
                .map(IncidentResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    private void verifyMonitorOwnership(UUID monitorId, UUID userId) {
        monitorService.getMonitor(monitorId, userId);
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        User user = userService.getOrCreateUser(clerkUserId, email != null ? email : clerkUserId, name != null ? name : "");
        return user.getId();
    }
}
