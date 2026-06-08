package akakash.backend.webhook;

import akakash.backend.user.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.svix.Webhook;
import com.svix.exceptions.WebhookVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpHeaders;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class ClerkWebhookController {

    @org.springframework.beans.factory.annotation.Value("${clerk.webhook-secret}")
    private String clerkWebhookSecret;

    private final UserService userService;
    private final ObjectMapper objectMapper;

    @PostMapping("/clerk")
    public ResponseEntity<Void> handleClerkWebhook(
            @RequestBody String payload,
            @RequestHeader("svix-id") String svixId,
            @RequestHeader("svix-signature") String svixSignature,
            @RequestHeader("svix-timestamp") String svixTimestamp
    ) {

        try {
            Webhook webhook = new Webhook(clerkWebhookSecret);

            // ✅ correct HttpHeaders (this is what your error is about)
            HttpHeaders headers = HttpHeaders.of(
                    Map.of(
                            "svix-id", List.of(svixId),
                            "svix-timestamp", List.of(svixTimestamp),
                            "svix-signature", List.of(svixSignature)
                    ),
                    (k, v) -> true
            );

            webhook.verify(payload, headers);

        } catch (WebhookVerificationException e) {
            log.error("Webhook verification failed: {}", e.getMessage());
            return ResponseEntity.status(400).build();
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }

        try {
            JsonNode event = objectMapper.readTree(payload);

            String type = event.get("type").asText();
            JsonNode data = event.get("data");

            String clerkUserId = data.get("id").asText();

            String email = "";
            if (data.has("email_addresses") && !data.get("email_addresses").isEmpty()) {
                email = data.get("email_addresses")
                        .get(0)
                        .get("email_address")
                        .asText();
            }

            String name = "";
            if (data.has("first_name") && data.has("last_name")) {
                name = data.get("first_name").asText() + " " + data.get("last_name").asText();
            }

            switch (type) {
                case "user.created" -> userService.createUser(clerkUserId, email, name);
                case "user.updated" -> userService.updateUser(clerkUserId, email, name);
                case "user.deleted" -> userService.deleteUser(clerkUserId);
                default -> log.info("Ignored event: {}", type);
            }

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed processing webhook: {}", e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}