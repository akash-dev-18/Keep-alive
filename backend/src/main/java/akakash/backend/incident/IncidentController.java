package akakash.backend.incident;

import akakash.backend.common.dto.IncidentResponse;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.user.User;
import akakash.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;
    private final MonitorRepository monitorRepository;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<IncidentResponse>> getIncidents(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "50") int limit) {
        UUID userId = getUserIdFromJwt(jwt);
        List<UUID> monitorIds = monitorRepository.findByUserId(userId).stream()
                .map(m -> m.getId()).toList();

        if (monitorIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }

        int cappedLimit = Math.min(Math.max(limit, 1), 100);
        List<IncidentResponse> incidents = incidentService.getIncidentsForUser(monitorIds, cappedLimit)
                .stream().map(IncidentResponse::fromEntity).toList();
        return ResponseEntity.ok(incidents);
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        User user = userService.getOrCreateUser(clerkUserId, email != null ? email : clerkUserId, name != null ? name : "");
        return user.getId();
    }
}
