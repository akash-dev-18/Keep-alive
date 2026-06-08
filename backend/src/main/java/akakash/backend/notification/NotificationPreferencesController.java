package akakash.backend.notification;

import akakash.backend.common.dto.NotificationPreferencesResponse;
import akakash.backend.common.dto.UpdateNotificationPreferencesRequest;
import akakash.backend.user.User;
import akakash.backend.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferencesService preferencesService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<NotificationPreferencesResponse> getPreferences(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(preferencesService.getPreferences(getUserIdFromJwt(jwt)));
    }

    @PutMapping
    public ResponseEntity<NotificationPreferencesResponse> updatePreferences(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateNotificationPreferencesRequest request) {
        return ResponseEntity.ok(preferencesService.updatePreferences(getUserIdFromJwt(jwt), request));
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        User user = userService.getOrCreateUser(clerkUserId, email != null ? email : clerkUserId, name != null ? name : "");
        return user.getId();
    }
}
