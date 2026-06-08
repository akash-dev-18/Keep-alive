package akakash.backend.alert;

import akakash.backend.common.dto.AlertLogResponse;
import akakash.backend.monitor.Monitor;
import akakash.backend.monitor.MonitorRepository;
import akakash.backend.user.User;
import akakash.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/alert-logs")
@RequiredArgsConstructor
public class AlertLogController {

    private final AlertLogRepository alertLogRepository;
    private final MonitorRepository monitorRepository;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<AlertLogResponse>> getAlertLogs(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserIdFromJwt(jwt);
        List<UUID> monitorIds = monitorRepository.findByUserId(userId)
                .stream().map(Monitor::getId).toList();
        if (monitorIds.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<AlertLog> logs = alertLogRepository.findByMonitorIdInOrderBySentAtDesc(monitorIds);
        List<AlertLogResponse> response = logs.stream()
                .map(AlertLogResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        User user = userService.getOrCreateUser(clerkUserId, email != null ? email : clerkUserId, name != null ? name : "");
        return user.getId();
    }
}
