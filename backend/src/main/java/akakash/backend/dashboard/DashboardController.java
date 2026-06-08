package akakash.backend.dashboard;

import akakash.backend.user.User;
import akakash.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import akakash.backend.common.dto.DashboardActivityResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getSummary(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserIdFromJwt(jwt);
        DashboardSummary summary = dashboardService.getSummary(userId);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/activity")
    public ResponseEntity<DashboardActivityResponse> getActivity(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "10") int limit) {
        UUID userId = getUserIdFromJwt(jwt);
        return ResponseEntity.ok(dashboardService.getActivity(userId, Math.min(Math.max(limit, 1), 50)));
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        User user = userService.getOrCreateUser(clerkUserId, email != null ? email : clerkUserId, name != null ? name : "");
        return user.getId();
    }
}
