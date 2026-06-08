package akakash.backend.webhook;

import akakash.backend.user.Plan;
import akakash.backend.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        
        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            String eventType = event.getType();

            switch (eventType) {
                case "checkout.session.completed" -> handleCheckoutCompleted(event);
                case "customer.subscription.created" -> handleSubscriptionCreated(event);
                case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
                case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
                default -> log.info("Unhandled Stripe event type: {}", eventType);
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to process Stripe webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    private void handleCheckoutCompleted(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().getObject().toJson());
            String customerId = data.path("customer").asText();
            String clientReferenceId = data.path("client_reference_id").asText();

            userRepository.findByClerkUserId(clientReferenceId).ifPresent(user -> {
                user.setStripeCustomerId(customerId);
                user.setUpdatedAt(OffsetDateTime.now());
                userRepository.save(user);
                log.info("Updated stripe_customer_id for user: {}", user.getEmail());
            });
        } catch (Exception e) {
            log.error("Error handling checkout.session.completed: {}", e.getMessage());
        }
    }

    private void handleSubscriptionCreated(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().getObject().toJson());
            String customerId = data.path("customer").asText();
            String subscriptionId = data.path("id").asText();
            String status = data.path("status").asText();

            userRepository.findByStripeCustomerId(customerId).ifPresentOrElse(
                    user -> {
                        user.setStripeSubscriptionId(subscriptionId);
                        if ("active".equals(status)) {
                            user.setPlan(Plan.PRO);
                        }
                        user.setUpdatedAt(OffsetDateTime.now());
                        userRepository.save(user);
                        log.info("Created subscription for user: {}", user.getEmail());
                    },
                    () -> log.warn("User not found for stripe customer ID: {}", customerId)
            );
        } catch (Exception e) {
            log.error("Error handling customer.subscription.created: {}", e.getMessage());
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().getObject().toJson());
            String customerId = data.path("customer").asText();
            String subscriptionId = data.path("id").asText();
            String status = data.path("status").asText();

            userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                user.setStripeSubscriptionId(subscriptionId);
                user.setPlan("active".equals(status) ? Plan.PRO : Plan.FREE);
                user.setUpdatedAt(OffsetDateTime.now());
                userRepository.save(user);
                log.info("Updated subscription for user: {}", user.getEmail());
            });
        } catch (Exception e) {
            log.error("Error handling customer.subscription.updated: {}", e.getMessage());
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        try {
            JsonNode data = objectMapper.readTree(event.getData().getObject().toJson());
            String customerId = data.path("customer").asText();

            userRepository.findByStripeCustomerId(customerId).ifPresent(user -> {
                user.setPlan(Plan.FREE);
                user.setStripeSubscriptionId(null);
                user.setUpdatedAt(OffsetDateTime.now());
                userRepository.save(user);
                log.info("Deleted subscription for user: {}", user.getEmail());
            });
        } catch (Exception e) {
            log.error("Error handling customer.subscription.deleted: {}", e.getMessage());
        }
    }
}
