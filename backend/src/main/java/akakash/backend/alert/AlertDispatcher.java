package akakash.backend.alert;

import akakash.backend.incident.Incident;
import akakash.backend.monitor.Monitor;
import akakash.backend.notification.Notification;
import akakash.backend.notification.NotificationPreferencesService;
import akakash.backend.notification.NotificationRepository;
import akakash.backend.user.User;
import akakash.backend.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertDispatcher {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final NotificationPreferencesService preferencesService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void dispatch(Monitor monitor, Incident incident, String alertType) {

        UUID userId = monitor.getUser().getId();
        User user = userRepository.findById(userId).orElseThrow();

        if (!preferencesService.shouldSendEmail(userId, alertType)) {
            return;
        }

        String dedupKey = buildDedupKey(monitor.getId(), alertType, incident);

        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofHours(1));

        if (Boolean.FALSE.equals(ok)) return;

        // save DB notification (UI notifications)
        notificationRepository.save(Notification.builder()
                .userId(userId)
                .monitorId(monitor.getId())
                .incidentId(incident != null ? incident.getId() : null)
                .title(buildTitle(alertType, monitor.getName()))
                .body(buildBody(alertType, monitor))
                .isRead(false)
                .build()
        );



        String alertReceiverEmail = user.getEmail(); // Check karo ki user.getEmail() sach mein string email de raha hai, id nahi!
        String targetMonitorName = monitor.getName();

        log.info("🚀 Preparing AlertJob payload -> Email: {}, Name: {}", alertReceiverEmail, targetMonitorName);

        AlertJob job = new AlertJob(
                monitor.getId(),                            // 1. monitorId
                incident != null ? incident.getId() : null, // 2. incidentId
                alertType,                                  // 3. alertType
                alertReceiverEmail,                         // 4. recipientEmail (Agar record mein email 4th par hai)
                targetMonitorName                           // 5. monitorName (Agar record mein name 5th par hai)
        );


        enqueueAfterCommit(job, dedupKey);
    }

    public void dispatchSslExpiry(Monitor monitor, int thresholdDays) {

        String dedupKey = "alert:sent:" + monitor.getId() + ":ssl_expiry:" + thresholdDays;

        Boolean firstTime = redisTemplate.opsForValue()
                .setIfAbsent(dedupKey, "1", Duration.ofDays(7));

        if (Boolean.FALSE.equals(firstTime)) {
            return;
        }

        // FIX: no threshold in alertType
        dispatch(monitor, null, "ssl_expiry");
    }

    private void enqueueAfterCommit(AlertJob job, String dedupKey) {

        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            enqueueNow(job);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {

                    @Override
                    public void afterCommit() {
                        enqueueNow(job);
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            redisTemplate.delete(dedupKey);
                        }
                    }
                }
        );
    }

    private void enqueueNow(AlertJob job) {
        try {
            String json = objectMapper.writeValueAsString(job);
            redisTemplate.opsForList().rightPush("queue:alerts", json);
        } catch (Exception e) {
            log.error("Failed to enqueue alert job", e);
        }
    }

    private String buildDedupKey(UUID monitorId, String alertType, Incident incident) {

        if ("recovery".equals(alertType) && incident != null) {
            return "alert:sent:" + monitorId + ":recovery:" + incident.getId();
        }

        return "alert:sent:" + monitorId + ":" + alertType;
    }

    private String buildTitle(String alertType, String monitorName) {
        return switch (alertType) {
            case "down" -> monitorName + " is DOWN";
            case "recovery" -> monitorName + " has recovered";
            case "ssl_expiry" -> monitorName + " SSL expiring soon";
            case "cron_missed" -> monitorName + " missed a heartbeat";
            default -> monitorName + " alert";
        };
    }

    private String buildBody(String alertType, Monitor monitor) {
        return switch (alertType) {
            case "down" ->
                    "Monitor " + monitor.getName() + " (" + monitor.getUrl() + ") is currently down.";

            case "recovery" ->
                    "Monitor " + monitor.getName() + " (" + monitor.getUrl() + ") has recovered.";

            case "ssl_expiry" ->
                    "SSL certificate for " + monitor.getUrl() + " is expiring soon.";

            case "cron_missed" ->
                    "Heartbeat monitor " + monitor.getName() + " missed its expected ping.";

            default ->
                    "Alert for monitor " + monitor.getName();
        };
    }
}