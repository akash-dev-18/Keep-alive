package akakash.backend.statuspage;

import akakash.backend.common.dto.CreateStatusPageRequest;
import akakash.backend.common.dto.StatusPageResponse;
import akakash.backend.common.dto.UpdateStatusPageRequest;
import akakash.backend.common.exception.ResourceNotFoundException;
import akakash.backend.user.User;
import akakash.backend.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/status-pages")
@RequiredArgsConstructor
public class StatusPageController {

    private final StatusPageService statusPageService;
    private final UserService userService;

    @PostMapping
    public ResponseEntity<StatusPageResponse> createStatusPage(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateStatusPageRequest request) {
        UUID userId = getUserIdFromJwt(jwt);
        StatusPage statusPage = statusPageService.createStatusPage(
                userId,
                request.name(),
                request.slug(),
                request.monitorIds(),
                request.isPublic() != null ? request.isPublic() : true,
                request.customDomain(),
                request.description(),
                request.logoUrl(),
                request.primaryColor()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(StatusPageResponse.fromEntity(statusPage));
    }

    @GetMapping
    public ResponseEntity<List<StatusPageResponse>> getStatusPages(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = getUserIdFromJwt(jwt);
        List<StatusPageResponse> response = statusPageService.getStatusPagesByUser(userId).stream()
                .map(StatusPageResponse::fromEntity)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{statusPageId}")
    public ResponseEntity<StatusPageResponse> getStatusPage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID statusPageId) {
        UUID userId = getUserIdFromJwt(jwt);
        StatusPage statusPage = statusPageService.getStatusPagesByUser(userId).stream()
                .filter(sp -> sp.getId().equals(statusPageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Status page not found: " + statusPageId));
        return ResponseEntity.ok(StatusPageResponse.fromEntity(statusPage));
    }

    @PutMapping("/{statusPageId}")
    public ResponseEntity<StatusPageResponse> updateStatusPage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID statusPageId,
            @RequestBody UpdateStatusPageRequest request) {
        UUID userId = getUserIdFromJwt(jwt);
        StatusPage statusPage = statusPageService.updateStatusPage(
                statusPageId,
                userId,
                request.name(),
                request.slug(),
                request.monitorIds(),
                request.isPublic() != null ? request.isPublic() : true,
                request.customDomain(),
                request.description(),
                request.logoUrl(),
                request.primaryColor()
        );
        return ResponseEntity.ok(StatusPageResponse.fromEntity(statusPage));
    }

    @DeleteMapping("/{statusPageId}")
    public ResponseEntity<Void> deleteStatusPage(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID statusPageId) {
        UUID userId = getUserIdFromJwt(jwt);
        statusPageService.deleteStatusPage(statusPageId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID getUserIdFromJwt(Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        User user = userService.getOrCreateUser(clerkUserId, email != null ? email : clerkUserId, name != null ? name : "");
        return user.getId();
    }
}
